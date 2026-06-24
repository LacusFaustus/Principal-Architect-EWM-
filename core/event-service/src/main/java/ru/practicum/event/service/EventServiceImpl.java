package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
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
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    // ==================== PRIVATE USER METHODS ====================

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        log.info("Getting events for user ID={}", userId);

        validateUserId(userId);
        validatePageable(pageable);

        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            log.info("No events found for user ID={}", userId);
            return Collections.emptyList();
        }

        Map<Long, UserInfoDto> usersMap = getUsersMap(events.getContent().stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toList()));

        Map<Long, Double> ratingsMap = getEventsRatings(events.getContent());
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events.getContent());

        List<EventShortDto> result = events.getContent().stream()
                .map(event -> {
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = createUserShortInfo(userDto, event.getInitiatorId());
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Found {} events for user ID={}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventFullDto postEvent(Long userId, NewEventDto newEventDto) {
        log.info("Creating event for user ID={}, title={}", userId, newEventDto.getTitle());

        validateUserId(userId);
        validateEventDate(newEventDto.getEventDate(), MIN_HOURS_BEFORE_EVENT);

        Category category = getCategoryOrThrow(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, userId);
        Event savedEvent = eventRepository.save(event);
        log.info("Event saved with ID={}", savedEvent.getId());

        UserInfoDto userDto = getUserOrThrow(userId);
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        return eventMapper.toEventFullDto(savedEvent, initiator, 0.0, 0L);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("Getting event for user ID={}, event ID={}", userId, eventId);

        validateUserId(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found or not accessible"));

        UserInfoDto userDto = getUserOrThrow(userId);
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(event, initiator, rating, confirmed);
    }

    @Override
    @Transactional
    @CachePut(value = "events", key = "#eventId")
    public EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Updating event by user: user ID={}, event ID={}", userId, eventId);

        validateUserId(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        validateEventStateForUpdate(event);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), MIN_HOURS_BEFORE_EVENT);
        }

        updateEventFields(event, updateRequest);
        handleUserStateAction(event, updateRequest.getStateAction());

        Event savedEvent = eventRepository.save(event);
        log.info("Event updated: ID={}, state={}", savedEvent.getId(), savedEvent.getState());

        UserInfoDto userDto = getUserOrThrow(userId);
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(savedEvent, initiator, rating, confirmed);
    }

    // ==================== ADMIN METHODS ====================

    @Override
    public List<EventFullDto> getEventsByAdminFilters(EventParams params) {
        log.info("Getting events by admin filters: users={}, states={}, categories={}",
                params.getUsers(), params.getStates(), params.getCategories());

        validatePageParams(params.getPageParams());

        Pageable pageable = buildPageable(params.getPageParams());
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
        Map<Long, Double> ratingsMap = getEventsRatings(events.getContent());
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events.getContent());

        List<EventFullDto> result = events.getContent().stream()
                .map(event -> {
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = createUserShortInfo(userDto, event.getInitiatorId());
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventFullDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());

        log.info("Found {} events matching admin filters", result.size());
        return result;
    }

    @Override
    @Transactional
    @CachePut(value = "events", key = "#eventId")
    public EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Updating event by admin: event ID={}", eventId);

        Event event = getEventOrThrow(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), MIN_HOURS_BEFORE_EVENT);
        }

        updateEventFields(event, updateRequest);
        handleAdminStateAction(event, updateRequest.getStateAction());

        Event savedEvent = eventRepository.save(event);
        log.info("Event updated by admin: ID={}, state={}", savedEvent.getId(), savedEvent.getState());

        UserInfoDto userDto = getUserOrThrow(event.getInitiatorId());
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(savedEvent, initiator, rating, confirmed);
    }

    // ==================== PUBLIC METHODS ====================

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, Long userId) {
        log.info("Getting events by public filters: userId={}, text={}, categories={}, paid={}",
                userId, params.getText(), params.getCategories(), params.getPaid());

        validatePageParams(params.getPageParams());

        LocalDateTime start = resolveRangeStart(params.getRangeStart(), params.getRangeEnd());
        LocalDateTime end = params.getRangeEnd();

        String text = normalizeText(params.getText());

        if ("VIEWS".equals(params.getSort())) {
            return getEventsSortedByRating(text, params.getCategories(), params.getPaid(),
                    start, end, params.getPageParams(), userId);
        }

        Pageable pageable = buildPageable(params.getPageParams());
        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                text, params.getCategories(), params.getPaid(), start, end, pageable);

        return buildEventShortDtos(eventsPage.getContent(), userId);
    }

    @Override
    @Cacheable(value = "events", key = "#eventId", unless = "#result == null")
    public EventFullDto getEventById(Long eventId, Long userId, HttpServletRequest request) {
        log.info("Getting event by ID: eventId={}, userId={}", eventId, userId);

        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            log.warn("Event not published: eventId={}, state={}", eventId, event.getState());
            throw new NotFoundException("Event must be published");
        }

        // Send view action to recommendation service
        sendViewActionIfUserProvided(userId, eventId);

        // Save stats
        saveStats(request);

        UserInfoDto userDto = getUserOrThrow(event.getInitiatorId());
        UserShortInfoDto initiator = new UserShortInfoDto(userDto.getId(), userDto.getName());

        Double rating = getEventRating(eventId);
        Long confirmed = getConfirmedRequests(eventId);

        return eventMapper.toEventFullDto(event, initiator, rating, confirmed);
    }

    @Override
    public List<EventShortDto> getRecommendationsForUser(Long userId, int size) {
        log.info("Getting recommendations for user: userId={}, size={}", userId, size);

        validateUserId(userId);

        if (size <= 0 || size > MAX_PAGE_SIZE) {
            log.warn("Invalid size: {}, using default: {}", size, DEFAULT_PAGE_SIZE);
            size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        }

        List<Long> recommendedEventIds = recommendationGrpcClient.getRecommendationsForUser(userId, size);

        if (recommendedEventIds.isEmpty()) {
            log.info("No recommendations found for user: {}", userId);
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findAllById(recommendedEventIds);

        if (events.isEmpty()) {
            log.info("No events found for recommended IDs: {}", recommendedEventIds);
            return Collections.emptyList();
        }

        // Preserve order from recommendations
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        Map<Long, UserInfoDto> usersMap = getUsersMap(events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList()));

        Map<Long, Double> ratingsMap = getEventsRatings(events);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events);

        List<EventShortDto> result = recommendedEventIds.stream()
                .filter(eventMap::containsKey)
                .map(eventId -> {
                    Event event = eventMap.get(eventId);
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = createUserShortInfo(userDto, event.getInitiatorId());
                    Double rating = ratingsMap.getOrDefault(eventId, 0.0);
                    Long confirmed = confirmedRequestsMap.getOrDefault(eventId, 0L);
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

        validateUserId(userId);

        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new BadRequestException("Cannot like unpublished event");
        }

        if (!hasUserInteractedWithEvent(userId, eventId)) {
            throw new BadRequestException("User must interact with event before liking");
        }

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
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                return false;
            }

            boolean isInitiator = event.getInitiatorId().equals(userId);
            Long confirmedRequests = requestFeignClient.countConfirmedRequestsByEventId(eventId);
            boolean hasRegistration = confirmedRequests != null && confirmedRequests > 0;

            boolean result = isInitiator || hasRegistration;
            log.debug("User interaction result: userId={}, eventId={}, isInitiator={}, hasRegistration={}, result={}",
                    userId, eventId, isInitiator, hasRegistration, result);
            return result;
        } catch (Exception e) {
            log.warn("Failed to check user interaction: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void saveStats(HttpServletRequest request) {
        try {
            NewEndpointHitDto hitDto = NewEndpointHitDto.builder()
                    .app("event-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statFeignClient.saveHit(hitDto);
            log.debug("Saved stats for request: {}", request.getRequestURI());
        } catch (Exception e) {
            log.error("Error saving stats: {}", e.getMessage());
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Invalid user ID: " + userId);
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }
    }

    private void validatePageParams(PageParams params) {
        if (params == null) {
            return;
        }
        if (params.getFrom() < 0) {
            throw new BadRequestException("From parameter must be >= 0");
        }
        if (params.getSize() != null && (params.getSize() <= 0 || params.getSize() > MAX_PAGE_SIZE)) {
            throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private Pageable buildPageable(PageParams params) {
        int page = params.getFrom() / (params.getSize() != null ? params.getSize() : DEFAULT_PAGE_SIZE);
        int size = params.getSize() != null ? params.getSize() : DEFAULT_PAGE_SIZE;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventDate"));
    }

    private LocalDateTime resolveRangeStart(LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (rangeStart == null && rangeEnd == null) {
            return LocalDateTime.now();
        }
        return rangeStart;
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate == null) {
            return;
        }
        LocalDateTime minDate = LocalDateTime.now().plusHours(hours);
        if (eventDate.isBefore(minDate)) {
            throw new BadRequestException(
                    String.format("Event date must be at least %d hours from now", hours)
            );
        }
    }

    private void validateEventStateForUpdate(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot change published event");
        }
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category " + categoryId + " not found"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
    }

    private UserInfoDto getUserOrThrow(Long userId) {
        try {
            UserInfoDto user = userFeignClient.getUserById(userId);
            if (user == null) {
                throw new NotFoundException("User " + userId + " not found");
            }
            return user;
        } catch (Exception e) {
            log.warn("User service unavailable for user {}: {}", userId, e.getMessage());
            UserInfoDto fallback = new UserInfoDto();
            fallback.setId(userId);
            fallback.setName("Unknown User");
            fallback.setEmail("unknown@example.com");
            return fallback;
        }
    }

    private UserShortInfoDto createUserShortInfo(UserInfoDto userDto, Long userId) {
        if (userDto != null) {
            return new UserShortInfoDto(userDto.getId(), userDto.getName());
        }
        return new UserShortInfoDto(userId, "Unknown User");
    }

    private void sendViewActionIfUserProvided(Long userId, Long eventId) {
        if (userId != null && userId > 0) {
            try {
                recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
                log.debug("Sent VIEW action to recommendation service: userId={}, eventId={}", userId, eventId);
            } catch (Exception e) {
                log.error("Failed to send VIEW action to recommendation service: {}", e.getMessage(), e);
            }
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest request) {
        if (request.getAnnotation() != null && !request.getAnnotation().isBlank()) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(new ru.practicum.location.model.LocationEntity(
                    request.getLocation().getLat(),
                    request.getLocation().getLon()
            ));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest request) {
        if (request.getAnnotation() != null && !request.getAnnotation().isBlank()) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(request.getCategory()));
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(new ru.practicum.location.model.LocationEntity(
                    request.getLocation().getLat(),
                    request.getLocation().getLon()
            ));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
    }

    private void handleUserStateAction(Event event, StateActionUser stateAction) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case SEND_TO_REVIEW:
                event.setState(EventState.PENDING);
                log.info("Event ID={} state changed to PENDING", event.getId());
                break;
            case CANCEL_REVIEW:
                event.setState(EventState.CANCELED);
                log.info("Event ID={} state changed to CANCELED", event.getId());
                break;
            default:
                log.warn("Unknown user state action: {}", stateAction);
        }
    }

    private void handleAdminStateAction(Event event, StateActionAdmin stateAction) {
        if (stateAction == null) {
            return;
        }
        switch (stateAction) {
            case PUBLISH_EVENT:
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish event because it's not in PENDING state");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                log.info("Event ID={} published", event.getId());
                break;
            case REJECT_EVENT:
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject event because it's already published");
                }
                event.setState(EventState.CANCELED);
                log.info("Event ID={} rejected", event.getId());
                break;
            default:
                log.warn("Unknown admin state action: {}", stateAction);
        }
    }

    private Double getEventRating(Long eventId) {
        try {
            Double rating = recommendationGrpcClient.getInteractionsCount(eventId);
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
            ratingsMap.put(event.getId(), getEventRating(event.getId()));
        }
        return ratingsMap;
    }

    private Map<Long, UserInfoDto> getUsersMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<UserInfoDto> users = userFeignClient.getUsersByIds(userIds);
            return users.stream().collect(Collectors.toMap(UserInfoDto::getId, Function.identity()));
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

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> confirmedRequestsMap = new HashMap<>();
        for (Event event : events) {
            try {
                Long count = requestFeignClient.countConfirmedRequestsByEventId(event.getId());
                confirmedRequestsMap.put(event.getId(), count != null ? count : 0L);
            } catch (Exception e) {
                log.warn("Request service unavailable for event {}: {}", event.getId(), e.getMessage());
                confirmedRequestsMap.put(event.getId(), 0L);
            }
        }
        return confirmedRequestsMap;
    }

    private Long getConfirmedRequests(Long eventId) {
        try {
            Long count = requestFeignClient.countConfirmedRequestsByEventId(eventId);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Request service unavailable for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private List<EventShortDto> buildEventShortDtos(List<Event> events, Long userId) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDto> usersMap = getUsersMap(initiatorIds);
        Map<Long, Double> ratingsMap = getEventsRatings(events);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events);

        return events.stream()
                .map(event -> {
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = createUserShortInfo(userDto, event.getInitiatorId());
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .collect(Collectors.toList());
    }

    private List<EventShortDto> getEventsSortedByRating(String text, List<Long> categories,
                                                        Boolean paid, LocalDateTime rangeStart,
                                                        LocalDateTime rangeEnd, PageParams pageParams,
                                                        Long userId) {
        log.debug("Getting events sorted by rating");

        // Get all events without pagination first
        Pageable allRecords = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("eventDate"));
        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                text, categories, paid, rangeStart, rangeEnd, allRecords);

        List<Event> events = eventsPage.getContent();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        // Get ratings for all events
        Map<Long, Double> ratingsMap = getEventsRatings(events);

        // Build DTOs
        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserInfoDto> usersMap = getUsersMap(initiatorIds);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    UserInfoDto userDto = usersMap.get(event.getInitiatorId());
                    UserShortInfoDto initiator = createUserShortInfo(userDto, event.getInitiatorId());
                    Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
                    Long confirmed = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    return eventMapper.toEventShortDto(event, initiator, rating, confirmed);
                })
                .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip(pageParams.getFrom())
                .limit(pageParams.getSize() != null ? pageParams.getSize() : DEFAULT_PAGE_SIZE)
                .collect(Collectors.toList());

        log.debug("Returning {} events sorted by rating", result.size());
        return result;
    }
}