package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.RecommendationGrpcClient;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.StatFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.dto.UserShortInfoDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.StateActionAdmin;
import ru.practicum.event.model.StateActionUser;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.BadRequestException;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;
import ru.practicum.stats.proto.ActionTypeProto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatFeignClient statFeignClient;
    private final UserFeignClient userFeignClient;
    private final RequestFeignClient requestFeignClient;
    private final RecommendationGrpcClient recommendationGrpcClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        log.info("GET events for user ID={}", userId);
        checkUserExists(userId);
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        Map<Long, UserInfoDto> usersMap = getUsersMap(events.getContent().stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toList()));

        List<EventShortDto> result = events.getContent().stream()
                .map(event -> {
                    Double rating = getEventRating(event.getId());
                    Long confirmed = getConfirmedRequests(event.getId());
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = userDto != null ?
                            new UserShortInfoDto(userDto.getId(), userDto.getName()) :
                            new UserShortInfoDto(event.getInitiatorId(), "Unknown User");
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Found {} events for user ID={}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    public EventFullDto postEvent(Long userId, NewEventDto newEventDto) {
        log.info("POST event for user ID={}, event title={}", userId, newEventDto.getTitle());

        validateEventDate(newEventDto.getEventDate(), 2);
        checkUserExists(userId);
        Category category = checkCategoryExists(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, userId);
        Event savedEvent = eventRepository.save(event);
        log.info("Event saved with ID={}", savedEvent.getId());

        UserInfoDto userDto = userFeignClient.getUserById(userId);
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Double rating = getEventRating(savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent, initiator, rating, 0L);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("GET event for user ID={}, event ID={}", userId, eventId);

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found or not accessible"));

        UserInfoDto userDto = userFeignClient.getUserById(userId);
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(event, initiator, rating, confirmed);
    }

    @Override
    @Transactional
    public EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("PATCH event by user: user ID={}, event ID={}", userId, eventId);

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() == EventState.PUBLISHED) {
            log.error("Cannot change published event: event ID={}, state={}", eventId, event.getState());
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 2);
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
                log.info("Event ID={} state changed to PENDING", eventId);
            } else {
                event.setState(EventState.CANCELED);
                log.info("Event ID={} state changed to CANCELED", eventId);
            }
        }

        UserInfoDto userDto = userFeignClient.getUserById(userId);
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Event savedEvent = eventRepository.save(event);
        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(savedEvent, initiator, rating, confirmed);
    }

    @Override
    public List<EventFullDto> getEventsByAdminFilters(EventParams params) {
        log.info("GET events by admin filters: users={}, states={}, categories={}",
                params.getUsers(), params.getStates(), params.getCategories());

        Pageable pageable = PageRequest.of(params.getPageParams().getFrom() / params.getPageParams().getSize(),
                params.getPageParams().getSize());

        Page<Event> events = eventRepository.findEventsByAdminFilters(
                params.getUsers(), params.getStates(), params.getCategories(),
                params.getRangeStart(), params.getRangeEnd(), pageable);

        if (events.isEmpty()) {
            log.info("No events found matching admin filters");
            return Collections.emptyList();
        }

        List<Long> initiatorIds = events.getContent().stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDto> usersMap = getUsersMap(initiatorIds);

        List<EventFullDto> result = events.stream()
                .map(event -> {
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = userDto != null ?
                            new UserShortInfoDto(userDto.getId(), userDto.getName()) :
                            new UserShortInfoDto(event.getInitiatorId(), "Unknown User");
                    Double rating = getEventRating(event.getId());
                    Long confirmed = getConfirmedRequests(event.getId());
                    return eventMapper.toEventFullDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Found {} events matching admin filters", result.size());
        return result;
    }

    @Override
    @Transactional
    public EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("PATCH event by admin: event ID={}", eventId);

        Event event = checkEventExists(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 1);
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    log.error("Cannot publish event: event ID={}, state={}", eventId, event.getState());
                    throw new ConflictException("Cannot publish event because it's not in PENDING state");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                log.info("Event ID={} published", eventId);
            } else {
                if (event.getState() == EventState.PUBLISHED) {
                    log.error("Cannot reject published event: event ID={}, state={}", eventId, event.getState());
                    throw new ConflictException("Cannot reject event because it's already published");
                }
                event.setState(EventState.CANCELED);
                log.info("Event ID={} rejected", eventId);
            }
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());

        UserInfoDto userDto = userFeignClient.getUserById(event.getInitiatorId());
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Event savedEvent = eventRepository.save(event);
        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(savedEvent, initiator, rating, confirmed);
    }

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, Long userId) {
        log.info("GET events by public filters: userId={}, text={}, categories={}, paid={}",
                userId, params.getText(), params.getCategories(), params.getPaid());

        LocalDateTime start = params.getRangeStart();
        LocalDateTime end = params.getRangeEnd();

        if (start == null && end == null) {
            start = LocalDateTime.now();
            log.debug("No range provided, using default start={}", start);
        }

        String text = (params.getText() != null && !params.getText().isBlank())
                ? params.getText() : null;

        int pageNum = params.getPageParams().getFrom() / params.getPageParams().getSize();
        Pageable pageable = PageRequest.of(pageNum, params.getPageParams().getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate"));

        if ("VIEWS".equals(params.getSort())) {
            log.debug("Sorting by rating (views)");
            return getEventsSortedByRating(params, start, userId);
        }

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                text, params.getCategories(), params.getPaid(), start, end, pageable);

        List<Event> events = eventsPage.getContent();

        if (events.isEmpty()) {
            log.info("No events found matching public filters");
            return Collections.emptyList();
        }

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDto> usersMap = getUsersMap(initiatorIds);

        // Получаем рейтинги для всех событий через рекомендательный сервис
        Map<Long, Double> ratingsMap = getEventsRatings(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    Long confirmed = getConfirmedRequests(event.getId());
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = userDto != null ?
                            new UserShortInfoDto(userDto.getId(), userDto.getName()) :
                            new UserShortInfoDto(event.getInitiatorId(), "Unknown User");
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Found {} events matching public filters", result.size());
        return result;
    }

    private List<EventShortDto> getEventsSortedByRating(PublicEventParams params, LocalDateTime rangeStart, Long userId) {
        log.debug("Getting events sorted by rating");

        Pageable allRecords = PageRequest.of(0, Integer.MAX_VALUE);

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), allRecords);

        List<Event> events = eventsPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // Получаем рейтинги для всех событий
        Map<Long, Double> ratingsMap = getEventsRatings(events);

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDto> usersMap = getUsersMap(initiatorIds);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = userDto != null ?
                            new UserShortInfoDto(userDto.getId(), userDto.getName()) :
                            new UserShortInfoDto(event.getInitiatorId(), "Unknown User");
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    Long confirmed = getConfirmedRequests(event.getId());
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip(params.getPageParams().getFrom())
                .limit(params.getPageParams().getSize())
                .collect(Collectors.toList());

        log.debug("Returning {} events sorted by rating", result.size());
        return result;
    }

    @Override
    public EventFullDto getEventById(Long eventId, Long userId, HttpServletRequest request) {
        log.info("GET event by ID: eventId={}, userId={}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() != EventState.PUBLISHED) {
            log.error("Event not published: eventId={}, state={}", eventId, event.getState());
            throw new NotFoundException("Event must be published");
        }

        // Отправляем действие просмотра в рекомендательный сервис, если userId указан
        if (userId != null && userId > 0) {
            try {
                recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
                log.debug("Sent VIEW action to recommendation service: userId={}, eventId={}", userId, eventId);
            } catch (Exception e) {
                log.error("Failed to send VIEW action to recommendation service: {}", e.getMessage(), e);
            }
        } else {
            log.debug("No userId provided, skipping VIEW action recording");
        }

        UserInfoDto userDto = userFeignClient.getUserById(event.getInitiatorId());
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        // Получаем рейтинг из рекомендательного сервиса
        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        EventFullDto result = eventMapper.toEventFullDto(event, initiator, rating, confirmed);
        log.debug("Returning event details for eventId={}, rating={}", eventId, rating);

        return result;
    }

    @Override
    public List<EventShortDto> getRecommendationsForUser(Long userId, int size) {
        log.info("Getting recommendations for user: userId={}, size={}", userId, size);

        List<Long> recommendedEventIds = recommendationGrpcClient.getRecommendationsForUser(userId, size);

        if (recommendedEventIds.isEmpty()) {
            log.info("No recommendations found for user: {}", userId);
            return Collections.emptyList();
        }

        log.debug("Received {} recommended event IDs for user: {}", recommendedEventIds.size(), userId);

        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        // Сохраняем порядок рекомендаций
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDto> usersMap = getUsersMap(initiatorIds);
        Map<Long, Double> ratingsMap = getEventsRatings(events);

        List<EventShortDto> result = recommendedEventIds.stream()
                .filter(eventMap::containsKey)
                .map(eventId -> {
                    Event event = eventMap.get(eventId);
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = userDto != null ?
                            new UserShortInfoDto(userDto.getId(), userDto.getName()) :
                            new UserShortInfoDto(event.getInitiatorId(), "Unknown User");
                    Double rating = ratingsMap.getOrDefault(eventId, 0.0);
                    Long confirmed = getConfirmedRequests(eventId);
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Returning {} recommendations for user: {}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        log.info("User liking event: userId={}, eventId={}", userId, eventId);

        // Проверяем существование события
        Event event = checkEventExists(eventId);

        // Проверяем, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            log.error("Cannot like unpublished event: eventId={}, state={}", eventId, event.getState());
            throw new BadRequestException("Cannot like unpublished event");
        }

        // Проверяем, взаимодействовал ли пользователь с событием
        if (!hasUserInteractedWithEvent(userId, eventId)) {
            log.error("User {} has not interacted with event {}", userId, eventId);
            throw new BadRequestException("User must interact with event before liking");
        }

        // Отправляем действие лайка в рекомендательный сервис
        try {
            recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
            log.info("Sent LIKE action to recommendation service: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.error("Failed to send LIKE action to recommendation service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process like", e);
        }
    }

    @Override
    public boolean hasUserInteractedWithEvent(Long userId, Long eventId) {
        log.debug("Checking if user interacted with event: userId={}, eventId={}", userId, eventId);

        try {
            // Проверяем через request-service, была ли регистрация
            Long confirmedRequests = requestFeignClient.countConfirmedRequestsByEventId(eventId);

            // Проверяем, не является ли пользователь организатором
            Event event = eventRepository.findById(eventId).orElse(null);

            boolean isInitiator = event != null && event.getInitiatorId().equals(userId);
            boolean hasRegistration = confirmedRequests != null && confirmedRequests > 0;

            boolean result = isInitiator || hasRegistration;
            log.debug("User interaction result: userId={}, eventId={}, isInitiator={}, hasRegistration={}, result={}",
                    userId, eventId, isInitiator, hasRegistration, result);

            return result;
        } catch (Exception e) {
            log.warn("Failed to check user interaction for userId={}, eventId={}: {}", userId, eventId, e.getMessage());
            return false;
        }
    }

    @Override
    public void saveStats(HttpServletRequest request) {
        try {
            statFeignClient.saveHit(new NewEndpointHitDto(
                    "event-service",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            ));
            log.debug("Saved stats for request: {}", request.getRequestURI());
        } catch (Exception e) {
            log.error("Error saving stats: {}", e.getMessage());
        }
    }

    private Double getEventRating(Long eventId) {
        try {
            Double rating = recommendationGrpcClient.getInteractionsCount(eventId);
            log.debug("Got rating for eventId={}: {}", eventId, rating);
            return rating != null ? rating : 0.0;
        } catch (Exception e) {
            log.warn("Failed to get rating for eventId={}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    private Map<Long, Double> getEventsRatings(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> ratingsMap = new HashMap<>();
        for (Event event : events) {
            Double rating = getEventRating(event.getId());
            ratingsMap.put(event.getId(), rating != null ? rating : 0.0);
        }
        return ratingsMap;
    }

    private Map<Long, UserInfoDto> getUsersMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserInfoDto> users = userFeignClient.getUsersByIds(userIds);
            Map<Long, UserInfoDto> result = users.stream().collect(Collectors.toMap(UserInfoDto::getId, u -> u));
            log.debug("Retrieved {} users from user service", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Failed to get users from user-service: {}", e.getMessage());
            Map<Long, UserInfoDto> fallbackMap = new HashMap<>();
            for (Long id : userIds) {
                UserInfoDto fallbackUser = new UserInfoDto();
                fallbackUser.setId(id);
                fallbackUser.setName("Unknown User");
                fallbackUser.setEmail("unknown@example.com");
                fallbackMap.put(id, fallbackUser);
            }
            return fallbackMap;
        }
    }

    private Long getConfirmedRequests(Long eventId) {
        try {
            Long count = requestFeignClient.countConfirmedRequestsByEventId(eventId);
            log.debug("Confirmed requests for eventId={}: {}", eventId, count);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Request service unavailable for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private void updateEventFields(Event event, String annotation, Long categoryId,
                                   String description, LocalDateTime eventDate,
                                   ru.practicum.location.dto.Location location, Boolean paid,
                                   Integer participantLimit, Boolean requestModeration, String title) {

        if (annotation != null && !annotation.isBlank()) {
            event.setAnnotation(annotation);
            log.debug("Updated annotation for event ID={}", event.getId());
        }
        if (categoryId != null) {
            event.setCategory(checkCategoryExists(categoryId));
            log.debug("Updated category for event ID={}", event.getId());
        }
        if (description != null && !description.isBlank()) {
            event.setDescription(description);
            log.debug("Updated description for event ID={}", event.getId());
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
            log.debug("Updated eventDate for event ID={}", event.getId());
        }
        if (location != null) {
            event.setLocation(new ru.practicum.location.model.LocationEntity(location.getLat(), location.getLon()));
            log.debug("Updated location for event ID={}", event.getId());
        }
        if (paid != null) {
            event.setPaid(paid);
            log.debug("Updated paid for event ID={}", event.getId());
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
            log.debug("Updated participantLimit for event ID={}", event.getId());
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
            log.debug("Updated requestModeration for event ID={}", event.getId());
        }
        if (title != null && !title.isBlank()) {
            event.setTitle(title);
            log.debug("Updated title for event ID={}", event.getId());
        }
    }

    private void checkUserExists(Long userId) {
        try {
            UserInfoDto user = userFeignClient.getUserById(userId);
            if (user != null) {
                log.debug("User exists: userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("User {} may not exist: {}", userId, e.getMessage());
        }
    }

    private Category checkCategoryExists(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category {} not found", catId);
                    return new NotFoundException("Category " + catId + " not found");
                });
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event {} not found", eventId);
                    return new NotFoundException("Event " + eventId + " not found");
                });
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            log.error("Event date too early: eventDate={}, required at least {} hours from now", eventDate, hours);
            throw new BadRequestException("Event date too early");
        }
    }
}