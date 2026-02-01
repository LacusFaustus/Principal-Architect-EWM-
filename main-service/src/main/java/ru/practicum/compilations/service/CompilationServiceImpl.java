package ru.practicum.compilations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatClient;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.CompilationSearchParam;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.mapper.CompilationMapper;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final EventMapper eventMapper;
    private final StatClient statClient;

    @Transactional
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

    @Transactional
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

    @Transactional
    public void delete(long compId) {
        log.info("Deleting compilation with id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }

        compilationRepository.deleteById(compId);
        log.info("Compilation deleted: {}", compId);
    }

    public CompilationDto get(long compId) {
        log.info("Getting compilation with id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        return buildCompilationDto(compilation);
    }

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

        Map<Long, Long> viewsMap = getEventsViews(events);
        
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventShortDto(event, views, confirmedRequests);
                })
                .collect(Collectors.toList());

        return compilationMapper.toCompilationDto(compilation, eventShortDtos);
    }
    
    private Map<Long, Long> getEventsViews(List<Event> events) {
        Map<Long, Long> views = new HashMap<>();

        if (events.isEmpty()) {
            return views;
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statClient.getStats(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now().plusYears(1),
                    uris.isEmpty() ? Collections.emptyList() : uris,
                    true);

            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                views.put(eventId, stat.getHits());
            }
        } catch (Exception e) {
            log.warn("Error getting stats from stats-service: {}. Returning 0 views for all events.", e.getMessage());
        }

        return views;
    }

    /**
     * Получение количества подтвержденных заявок на участие.
     * Временная реализация - всегда возвращает 0.
     * TODO: Реализовать получение данных из ParticipationRequestRepository
     */
    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        Map<Long, Long> confirmedRequests = new HashMap<>();
        
        // Временная реализация - возвращаем 0 для всех событий
        for (Event event : events) {
            confirmedRequests.put(event.getId(), 0L);
        }
        
        return confirmedRequests;
    }
}
