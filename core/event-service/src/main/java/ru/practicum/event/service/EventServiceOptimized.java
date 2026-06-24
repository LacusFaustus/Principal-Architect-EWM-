package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.StatFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.dto.UserShortInfoDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;

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
public class EventServiceOptimized {

    private final EventMapper eventMapper;
    private final StatFeignClient statFeignClient;
    private final UserFeignClient userFeignClient;
    private final RequestFeignClient requestFeignClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Оптимизированный метод получения событий с batch-запросами
     */
    public List<EventShortDto> getEventsOptimized(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Optimizing {} events with batch requests", events.size());

        // 1. Получаем все ID пользователей
        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        // 2. Получаем все ID событий
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // 3. Параллельные запросы к внешним сервисам
        CompletableFuture<Map<Long, UserInfoDto>> usersFuture =
                CompletableFuture.supplyAsync(() -> getUsersBatch(userIds), executorService);

        CompletableFuture<Map<Long, Long>> viewsFuture =
                CompletableFuture.supplyAsync(() -> getViewsBatch(eventIds), executorService);

        CompletableFuture<Map<Long, Long>> confirmedRequestsFuture =
                CompletableFuture.supplyAsync(() -> getConfirmedRequestsBatch(eventIds), executorService);

        // 4. Ожидаем результаты
        try {
            CompletableFuture.allOf(usersFuture, viewsFuture, confirmedRequestsFuture).join();

            Map<Long, UserInfoDto> usersMap = usersFuture.get();
            Map<Long, Long> viewsMap = viewsFuture.get();
            Map<Long, Long> confirmedRequestsMap = confirmedRequestsFuture.get();

            // 5. Собираем результат
            return events.stream()
                    .map(event -> {
                        UserInfoDto user = usersMap.get(event.getInitiatorId());
                        UserShortInfoDto initiator = createUserShortInfo(user, event.getInitiatorId());
                        Long views = viewsMap.getOrDefault(event.getId(), 0L);
                        Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                        Double rating = views.doubleValue();
                        return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting optimized events", e);
            return events.stream()
                    .map(event -> {
                        UserShortInfoDto initiator = new UserShortInfoDto(event.getInitiatorId(), "Unknown User");
                        return eventMapper.toEventShortDto(event, initiator, 0.0, 0L);
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Batch запрос к User Service
     */
    private Map<Long, UserInfoDto> getUsersBatch(List<Long> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return Collections.emptyMap();
            }

            // Разбиваем на пачки по 100 пользователей
            List<List<Long>> batches = partitionList(userIds, 100);

            Map<Long, UserInfoDto> result = new HashMap<>();
            for (List<Long> batch : batches) {
                try {
                    List<UserInfoDto> users = userFeignClient.getUsersByIds(batch);
                    if (users != null) {
                        users.forEach(user -> result.put(user.getId(), user));
                    }
                } catch (Exception e) {
                    log.warn("Failed to get users batch: {}", e.getMessage());
                }
            }

            log.debug("Got {} users from {} IDs", result.size(), userIds.size());
            return result;

        } catch (Exception e) {
            log.error("Error getting users batch", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Batch запрос к Stats Service
     */
    private Map<Long, Long> getViewsBatch(List<Long> eventIds) {
        try {
            if (eventIds == null || eventIds.isEmpty()) {
                return Collections.emptyMap();
            }

            // Формируем URI для запроса
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            // Разбиваем на пачки
            List<List<String>> uriBatches = partitionList(uris, 50);

            Map<Long, Long> viewsMap = new HashMap<>();

            for (List<String> uriBatch : uriBatches) {
                try {
                    List<ViewStatsDto> stats = statFeignClient.getStats(
                            LocalDateTime.now().minusMonths(1).format(FORMATTER),
                            LocalDateTime.now().format(FORMATTER),
                            uriBatch,
                            true
                    );

                    if (stats != null) {
                        for (ViewStatsDto stat : stats) {
                            String uri = stat.getUri();
                            Long eventId = extractEventIdFromUri(uri);
                            if (eventId != null) {
                                viewsMap.merge(eventId, stat.getHits(), Long::sum);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get stats batch: {}", e.getMessage());
                }
            }

            // Для событий без просмотров - ставим 0
            for (Long eventId : eventIds) {
                viewsMap.putIfAbsent(eventId, 0L);
            }

            log.debug("Got views for {} events", viewsMap.size());
            return viewsMap;

        } catch (Exception e) {
            log.error("Error getting views batch", e);
            // Возвращаем карту с нулями
            return eventIds.stream()
                    .collect(Collectors.toMap(Function.identity(), id -> 0L));
        }
    }

    /**
     * Batch запрос к Request Service
     */
    private Map<Long, Long> getConfirmedRequestsBatch(List<Long> eventIds) {
        Map<Long, Long> result = new HashMap<>();

        try {
            if (eventIds == null || eventIds.isEmpty()) {
                return result;
            }

            // Используем параллельные запросы
            List<CompletableFuture<Void>> futures = eventIds.stream()
                    .map(eventId -> CompletableFuture.runAsync(() -> {
                        try {
                            Long count = requestFeignClient.countConfirmedRequestsByEventId(eventId);
                            synchronized (result) {
                                result.put(eventId, count != null ? count : 0L);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get confirmed requests for event {}: {}", eventId, e.getMessage());
                            synchronized (result) {
                                result.put(eventId, 0L);
                            }
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error("Error getting confirmed requests batch", e);
            // В случае ошибки возвращаем нули
            for (Long eventId : eventIds) {
                result.put(eventId, 0L);
            }
        }

        return result;
    }

    /**
     * Вспомогательные методы
     */
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

    private UserShortInfoDto createUserShortInfo(UserInfoDto userDto, Long userId) {
        if (userDto != null) {
            return new UserShortInfoDto(userDto.getId(), userDto.getName());
        }
        return new UserShortInfoDto(userId, "Unknown User");
    }
}