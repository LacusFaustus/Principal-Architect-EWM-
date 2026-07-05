package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.StatFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.CompilationSearchParam;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.dto.UserShortInfoDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.NotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final StatFeignClient statFeignClient;
    private final UserFeignClient userFeignClient;
    private final RequestFeignClient requestFeignClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    @Transactional
    @CacheEvict(value = "compilations", allEntries = true)
    public CompilationDto add(NewCompilationDto newCompilationDto) {
        log.info("Adding new compilation: {}", newCompilationDto.getTitle());

        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
            compilation.setEvents(events);
        }

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Compilation saved with id: {}", savedCompilation.getId());

        return buildCompilationDto(savedCompilation);
    }

    @Override
    @Transactional
    @CacheEvict(value = "compilations", key = "#compId")
    public CompilationDto update(long compId, UpdateCompilationRequest updateRequest) {
        log.info("Updating compilation with id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Compilation updated: {}", updatedCompilation.getId());

        return buildCompilationDto(updatedCompilation);
    }

    @Override
    @Transactional
    @CacheEvict(value = "compilations", key = "#compId")
    public void delete(long compId) {
        log.info("Deleting compilation with id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }

        compilationRepository.deleteById(compId);
        log.info("Compilation deleted: {}", compId);
    }

    @Override
    @Cacheable(value = "compilations", key = "#compId", unless = "#result == null")
    public CompilationDto get(long compId) {
        log.info("Getting compilation with id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        return buildCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(CompilationSearchParam params) {
        log.info("Getting compilations with params: pinned={}, from={}, size={}",
                params.getPinned(), params.getFrom(), params.getSize());

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        List<Compilation> compilations = compilationRepository.findAllByPinned(params.getPinned(), pageable);

        log.info("Found {} compilations", compilations.size());

        return compilations.stream()
                .map(this::buildCompilationDto)
                .collect(Collectors.toList());
    }

    private CompilationDto buildCompilationDto(Compilation compilation) {
        List<Event> events = new ArrayList<>(compilation.getEvents());

        if (events.isEmpty()) {
            return compilationMapper.toCompilationDto(compilation, Collections.emptyList());
        }

        // Используем оптимизированный batch-подход
        Map<Long, Long> viewsMap = getEventsViewsOptimized(events);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsOptimized(events);

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserShortInfoDto> initiatorsMap = getInitiatorsMapOptimized(initiatorIds);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    Double rating = views != null ? views.doubleValue() : 0.0;
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    UserShortInfoDto initiator = initiatorsMap.getOrDefault(
                            event.getInitiatorId(),
                            new UserShortInfoDto(event.getInitiatorId(), "Unknown User")
                    );
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmedRequests);
                })
                .collect(Collectors.toList());

        return compilationMapper.toCompilationDto(compilation, eventShortDtos);
    }

    private Map<Long, UserShortInfoDto> getInitiatorsMapOptimized(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserInfoDto> users = userFeignClient.getUsersByIds(userIds);
            return users.stream()
                    .collect(Collectors.toMap(
                            UserInfoDto::getId,
                            u -> new UserShortInfoDto(u.getId(), u.getName())
                    ));
        } catch (Exception e) {
            log.warn("Failed to get users from user-service: {}", e.getMessage());
            Map<Long, UserShortInfoDto> fallbackMap = new HashMap<>();
            for (Long id : userIds) {
                fallbackMap.put(id, new UserShortInfoDto(id, "Unknown User"));
            }
            return fallbackMap;
        }
    }

    private Map<Long, Long> getEventsViewsOptimized(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = new HashMap<>();

        try {
            // Разбиваем на пачки по 50 URI для оптимизации
            List<List<String>> uriBatches = partitionList(uris, 50);

            List<CompletableFuture<Void>> futures = uriBatches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> {
                        try {
                            List<ViewStatsDto> stats = statFeignClient.getStats(
                                    LocalDateTime.now().minusMonths(1).format(FORMATTER),
                                    LocalDateTime.now().format(FORMATTER),
                                    batch,
                                    true
                            );

                            if (stats != null) {
                                synchronized (viewsMap) {
                                    for (ViewStatsDto stat : stats) {
                                        Long eventId = extractEventIdFromUri(stat.getUri());
                                        if (eventId != null) {
                                            viewsMap.merge(eventId, stat.getHits(), Long::sum);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get stats batch: {}", e.getMessage());
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Для событий без просмотров - ставим 0
            for (Long eventId : eventIds) {
                viewsMap.putIfAbsent(eventId, 0L);
            }

        } catch (Exception e) {
            log.error("Error getting views batch", e);
            // Возвращаем карту с нулями
            for (Long eventId : eventIds) {
                viewsMap.put(eventId, 0L);
            }
        }

        return viewsMap;
    }

    private Map<Long, Long> getConfirmedRequestsOptimized(List<Event> events) {
        Map<Long, Long> result = new HashMap<>();

        if (events.isEmpty()) {
            return result;
        }

        try {
            List<CompletableFuture<Void>> futures = events.stream()
                    .map(event -> CompletableFuture.runAsync(() -> {
                        try {
                            Long count = requestFeignClient.countConfirmedRequestsByEventId(event.getId());
                            synchronized (result) {
                                result.put(event.getId(), count != null ? count : 0L);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get confirmed requests for event {}: {}", event.getId(), e.getMessage());
                            synchronized (result) {
                                result.put(event.getId(), 0L);
                            }
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error("Error getting confirmed requests batch", e);
            for (Event event : events) {
                result.put(event.getId(), 0L);
            }
        }

        return result;
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    private Long extractEventIdFromUri(String uri) {
        if (uri == null || !uri.startsWith("/events/")) {
            return null;
        }
        try {
            return Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}