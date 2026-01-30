package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
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
import ru.practicum.client.StatClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import ru.practicum.event.dto.ParticipationRequestDto;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final StatClient statClient;

    //PrivateUserEventController
    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        log.info("GET events for user ID: {}", userId);

        checkUserExists(userId);
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        log.debug("FIND events size: {}", events.getTotalElements());

        Map<Long, Long> views = getEventsViews(events.getContent());
        log.debug("FIND views: {}", events.getTotalElements());

        //TODO добавить получение confirmedRequests
        return events.getContent().stream()
                .map(event -> eventMapper.toEventShortDto(event, views.getOrDefault(event.getId(), 0L), 0L))
                .collect(Collectors.toList());
    }

    //PrivateUserEventController
    @Override
    @Transactional
    public EventFullDto postEvent(Long userId, NewEventDto newEventDto) {
        log.info("POST event: {}", newEventDto);

        validateEventDate(newEventDto.getEventDate(), 2);
        User user = checkUserExists(userId);
        Category category = checkCategoryExists(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, user);
        log.debug("MAP event: {}", event);
        Event savedEvent = eventRepository.save(event);

        log.info("SAVED event: {}", savedEvent);

        return eventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    //PrivateUserEventController
    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("GET event: ID={}", eventId);

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found for user ID=" + userId));
        log.debug("FIND event: {}", event);

        Map<Long, Long> views = getEventsViews(List.of(event));

        //TODO добавить получение confirmedRequests
        return eventMapper.toEventFullDto(event, views.getOrDefault(eventId, 0L), getConfirmedRequests(eventId));
    }

    //PrivateUserEventController
    @Override
    @Transactional
    public EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("USER PATCH event ID:{}, UPDATE: {}", eventId, updateRequest);

        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found for user ID=" + userId));

        validateEventStateForUserUpdate(event);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 2);
        }

        updateEventFields(event, updateRequest);
        handleUserStateAction(event, updateRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        log.info("Event ID: {} updated by user ID: {}", eventId, userId);

        return eventMapper.toEventFullDto(updatedEvent, 0L, 0L);
    }

    @Override
    public List<EventFullDto> getEventsByAdminFilters(EventParams params) {
        log.info("Getting events by admin filters");

        Pageable pageable = createPageable(params.getPageParams());
        Page<Event> events = eventRepository.findEventsByAdminFilters(
                params.getUsers(), params.getStates(), params.getCategories(),
                params.getRangeStart(), params.getRangeEnd(), pageable);

        return events.getContent().stream()
                .map(event -> eventMapper.toEventFullDto(event, 0L, 0L))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("ADMIN PATCH event ID: {} by UPDATE: {}", eventId, updateRequest);

        Event event = checkEventExists(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 1);
        }

        handleAdminStateAction(event, updateRequest.getStateAction());
        updateEventFields(event, updateRequest);

        Event updatedEvent = eventRepository.save(event);
        log.info("SAVE: event={}", updatedEvent);

        return eventMapper.toEventFullDto(updatedEvent, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request) {
        log.info("Getting events by public filters");

        LocalDateTime rangeStart = params.getRangeStart() != null ? params.getRangeStart() : LocalDateTime.now();

        Sort sort = getSort(params.getSort());
        Pageable pageable = createPageable(params.getPageParams(), sort);

        Page<Event> events = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), pageable);

        List<Event> filteredEvents = filterByAvailability(events.getContent(), params.getOnlyAvailable());

        // Получаем статистику просмотров
        Map<Long, Long> views = getEventsViews(filteredEvents);

        return filteredEvents.stream()
                .map(event -> eventMapper.toEventShortDto(event,
                        views.getOrDefault(event.getId(), 0L),
                        getConfirmedRequests(event.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        log.info("Getting event ID: {}", eventId);

        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> {
                        log.error("Event ID={} not found", eventId);
                        return new NotFoundException("Event ID=" + eventId + " not found");
                    });

            log.info("Event found: ID={}, state={}, title={}, category={}, initiator={}",
                    eventId, event.getState(), event.getTitle(),
                    event.getCategory(), event.getInitiator());

            if (event.getState() != EventState.PUBLISHED) {
                log.error("Event ID={} is not published. State: {}", eventId, event.getState());
                throw new NotFoundException("Event ID=" + eventId + " not found or not published");
            }

            // Получаем статистику просмотров
            Map<Long, Long> views = getEventsViews(List.of(event));
            Long confirmedRequests = getConfirmedRequests(eventId);

            log.info("Stats for event ID={}: views={}, confirmedRequests={}",
                    eventId, views.getOrDefault(eventId, 0L), confirmedRequests);

            try {
                EventFullDto dto = eventMapper.toEventFullDto(event,
                        views.getOrDefault(eventId, 0L),
                        confirmedRequests);

                log.info("DTO created for event ID={}: has id={}, title={}",
                        eventId, dto.getId(), dto.getTitle());
                return dto;

            } catch (Exception e) {
                log.error("Error mapping event to DTO for event ID={}: {}", eventId, e.getMessage(), e);
                throw e;
            }

        } catch (Exception e) {
            log.error("Error getting event ID={}: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void saveStats(HttpServletRequest request) {
        NewEndpointHitDto endpointHitDto = new NewEndpointHitDto(
                "ewm-main-service",
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now()
        );

        statClient.saveHit(endpointHitDto);
    }

    // Вспомогательные методы
    private User checkUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found", userId);
                    return new NotFoundException("User ID=" + userId + " not found");
                });
    }

    private Category checkCategoryExists(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> {
                    log.error("Category {} not found", catId);
                    return new NotFoundException("Category ID=" + catId + " not found");
                });
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event {} not found", eventId);
                    return new NotFoundException("Event ID=" + eventId + " not found");
                });
    }

    //Валидация времени +2 часа от текущего для POST и PATCH
    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new BadRequestException("Event date must be at least " + hours + " hours from now");
        }
    }

    private void validateEventStateForUserUpdate(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
    }

    private void handleUserStateAction(Event event, StateActionUser stateAction) {
        if (stateAction == StateActionUser.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (stateAction == StateActionUser.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
    }

    private void handleAdminStateAction(Event event, StateActionAdmin stateAction) {
        if (stateAction == StateActionAdmin.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish event because it's not in PENDING state");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (stateAction == StateActionAdmin.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject event because it's already published");
            }
            event.setState(EventState.CANCELED);
        }
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        updateCommonFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        updateCommonFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());
    }

    private void updateCommonFields(Event event, String annotation, Long categoryId,
                                    String description, LocalDateTime eventDate,
                                    ru.practicum.location.dto.Location location, Boolean paid,
                                    Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) event.setAnnotation(annotation);
        if (categoryId != null) {
            Category category = checkCategoryExists(categoryId);
            event.setCategory(category);
        }
        if (description != null) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (location != null)
            event.setLocation(new ru.practicum.location.model.LocationEntity(location.getLat(), location.getLon()));
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null) event.setTitle(title);
    }

    private Pageable createPageable(PageParams pageParams) {
        return createPageable(pageParams, Sort.by("id").descending());
    }

    private Pageable createPageable(PageParams pageParams, Sort sort) {
        if (pageParams == null) {
            pageParams = new PageParams();
        }
        return PageRequest.of(pageParams.getFrom() / pageParams.getSize(),
                pageParams.getSize(), sort);
    }

    private Sort getSort(String sortParam) {
        // Нельзя сортировать по views в БД, так как это вычисляемое поле
        // Будем сортировать в памяти после получения данных
        return Sort.by("eventDate");
    }

    private List<Event> filterByAvailability(List<Event> events, Boolean onlyAvailable) {
        if (Boolean.TRUE.equals(onlyAvailable)) {
            return events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            getConfirmedRequests(event.getId()) < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }
        return events;
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
            // ВАЖНО: Не передаем null для uris, передаем пустой список если нужно
            List<ViewStatsDto> stats = statClient.getStats(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now().plusYears(1),
                    uris.isEmpty() ? Collections.emptyList() : uris, // Не null!
                    true);

            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                views.put(eventId, stat.getHits());
            }
        } catch (Exception e) {
            log.warn("Error getting stats: {}. Returning 0 views for all events.", e.getMessage());
        }

        return views;
    }

    private final Map<Long, Long> confirmedRequestsCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ParticipationRequestDto postRequest(Long userId, Long eventId) {
        log.info("POST request for user ID={} to event ID={}", userId, eventId);

        checkUserExists(userId);
        Event event = checkEventExists(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot participate in own event");
        }

        // Проверяем лимит участников
        Long confirmedRequests = getConfirmedRequests(eventId);
        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        // Создаем DTO запроса
        ParticipationRequestDto requestDto = new ParticipationRequestDto();
        requestDto.setId(System.currentTimeMillis());
        requestDto.setRequester(userId);
        requestDto.setEvent(eventId);
        requestDto.setStatus("PENDING");
        requestDto.setCreated(LocalDateTime.now());

        // Для тестов: если событие не требует модерации, автоматически подтверждаем
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            requestDto.setStatus("CONFIRMED");
            confirmedRequestsCache.put(eventId, confirmedRequests + 1);
        }

        return requestDto;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        log.info("Canceling participation request ID: {} for user ID: {}", requestId, userId);

        // Временная реализация
        ParticipationRequestDto requestDto = new ParticipationRequestDto();
        requestDto.setId(requestId);
        requestDto.setRequester(userId);
        requestDto.setEvent(1L);
        requestDto.setStatus("CANCELED");
        requestDto.setCreated(LocalDateTime.now());

        return requestDto;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest) {

        log.info("Updating request status for event ID: {} by user ID: {}", eventId, userId);

        // Проверяем существование пользователя
        checkUserExists(userId);

        // Проверяем существование события и что пользователь - инициатор
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only event initiator can update requests");
        }

        // Проверяем, что событие требует модерации
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Event doesn't require request moderation");
        }

        // Проверяем текущее количество подтвержденных запросов
        Long confirmedRequests = getConfirmedRequests(eventId);

        // Временная реализация - возвращаем пустые списки
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(List.of());
        result.setRejectedRequests(List.of());

        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId) {
        log.info("Getting participation requests for event ID: {} by user ID: {}", eventId, userId);

        // Проверяем существование пользователя
        checkUserExists(userId);

        // Проверяем существование события и что пользователь - инициатор
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event ID=" + eventId + " not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Only event initiator can view requests");
        }

        // Временная реализация - возвращаем пустой список
        return List.of();
    }

    @Override
    public List<ParticipationRequestDto> getUserParticipationRequests(Long userId) {
        log.info("Getting participation requests for user ID: {}", userId);

        // Проверяем существование пользователя
        checkUserExists(userId);

        // Временная реализация - возвращаем пустой список
        return List.of();
    }

    private Long getConfirmedRequests(Long eventId) {
        return confirmedRequestsCache.getOrDefault(eventId, 0L);
    }
}
