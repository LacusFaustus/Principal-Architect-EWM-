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
import ru.practicum.client.StatClient;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
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
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final StatClient statClient;

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        checkUserExists(userId);
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return events.getContent().stream()
                .map(event -> {
                    Long views = getViews(event.getId());
                    Long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestState.CONFIRMED);
                    return eventMapper.toEventShortDto(event, views, confirmed);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto postEvent(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate(), 2);

        User user = checkUserExists(userId);
        Category category = checkCategoryExists(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto, category, user);
        Event savedEvent = eventRepository.save(event);

        return eventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found or not accessible"));

        return eventMapper.toEventFullDto(event, getViews(eventId),
                requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED));
    }

    @Override
    @Transactional
    public EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        checkUserExists(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() == EventState.PUBLISHED) {
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
            } else {
                event.setState(EventState.CANCELED);
            }
        }

        return eventMapper.toEventFullDto(eventRepository.save(event), getViews(eventId),
                requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED));
    }

    @Override
    public List<EventFullDto> getEventsByAdminFilters(EventParams params) {
        Pageable pageable = PageRequest.of(params.getPageParams().getFrom() / params.getPageParams().getSize(),
                params.getPageParams().getSize());

        Page<Event> events = eventRepository.findEventsByAdminFilters(
                params.getUsers(), params.getStates(), params.getCategories(),
                params.getRangeStart(), params.getRangeEnd(), pageable);

        return events.stream()
                .map(event -> eventMapper.toEventFullDto(event, getViews(event.getId()),
                        requestRepository.countByEventIdAndStatus(event.getId(), RequestState.CONFIRMED)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = checkEventExists(eventId);

        if (updateRequest.getEventDate() != null) {
            validateEventDate(updateRequest.getEventDate(), 1);
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish event because it's not in PENDING state");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject event because it's already published");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(),
                updateRequest.getRequestModeration(), updateRequest.getTitle());

        return eventMapper.toEventFullDto(eventRepository.save(event), getViews(eventId),
                requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED));
    }

    @Override
    public List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request) {
        LocalDateTime rangeStart = params.getRangeStart() != null ? params.getRangeStart() : LocalDateTime.now();

        int from = (params.getPageParams() != null && params.getPageParams().getFrom() != null)
                ? params.getPageParams().getFrom() : 0;
        int size = (params.getPageParams() != null && params.getPageParams().getSize() != null)
                ? params.getPageParams().getSize() : 10;

        if (params.getRangeEnd() != null && params.getRangeEnd().isBefore(rangeStart)) {
            throw new BadRequestException("RangeEnd must be after RangeStart");
        }

        if ("VIEWS".equals(params.getSort())) {
            return getEventsSortedByViews(params, rangeStart);
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), params.getOnlyAvailable(), pageable);

        return eventsPage.getContent().stream()
                .map(event -> eventMapper.toEventShortDto(event, getViews(event.getId()),
                        requestRepository.countByEventIdAndStatus(event.getId(), RequestState.CONFIRMED)))
                .collect(Collectors.toList());
    }

    private List<EventShortDto> getEventsSortedByViews(PublicEventParams params, LocalDateTime rangeStart) {
        Pageable allRecords = PageRequest.of(0, Integer.MAX_VALUE);

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), params.getOnlyAvailable(), allRecords);

        List<Event> events = eventsPage.getContent();
        Map<Long, Long> viewsMap = getEventsViews(events); // Наш метод получения стат

        return events.stream()
                .map(event -> eventMapper.toEventShortDto(event,
                        viewsMap.getOrDefault(event.getId(), 0L),
                        getConfirmedRequests(event.getId())))
                .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                .skip(params.getPageParams().getFrom())
                .limit(params.getPageParams().getSize())
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        saveStats(request);
        return eventMapper.toEventFullDto(event, getViews(eventId),
                requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED));
    }

    @Override
    public void saveStats(HttpServletRequest request) {
        statClient.saveHit(new NewEndpointHitDto(
                "ewm-main-service", request.getRequestURI(), request.getRemoteAddr(), LocalDateTime.now()));
    }

    @Override
    @Transactional
    public ParticipationRequestDto postRequest(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event is not published");
        }

        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        Request request = Request.builder()
                .requester(requester)
                .event(event)
                .created(LocalDateTime.now())
                .status((!event.getRequestModeration() || event.getParticipantLimit() == 0) ?
                        RequestState.CONFIRMED : RequestState.PENDING)
                .build();

        return requestMapper.mapToRequestDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        User user = checkUserExists(userId);

        if (!request.getRequester().equals(user)) {
            throw new NotFoundException("Request does not belong to user");
        }

        request.setStatus(RequestState.CANCELED);
        return requestMapper.mapToRequestDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId,
                                                                    EventRequestStatusUpdateRequest updateReq) {
        checkUserExists(userId);
        Event event = checkEventExists(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User is not initiator");
        }

        List<Request> requests = requestRepository.findAllByIdIn(updateReq.getRequestIds());

        // Валидация: статус можно менять только у PENDING
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestState.PENDING)) {
            throw new ConflictException("Requests must be in PENDING state");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (updateReq.getStatus().equals("REJECTED")) {
            requests.forEach(r -> {
                r.setStatus(RequestState.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(r));
            });
            requestRepository.saveAll(requests);
        } else {
            // Логика подтверждения с контролем лимита
            Long currentCount = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
            long limit = event.getParticipantLimit();

            if (limit > 0 && currentCount >= limit) {
                throw new ConflictException("Limit reached");
            }

            for (Request r : requests) {
                if (limit > 0 && currentCount >= limit) {
                    // Если лимит кончился прямо в процессе - отклоняем оставшиеся
                    r.setStatus(RequestState.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(r));
                } else {
                    r.setStatus(RequestState.CONFIRMED);
                    confirmed.add(requestMapper.mapToRequestDto(r));
                    currentCount++;
                }
            }
            requestRepository.saveAll(requests);
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = checkEventExists(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User is not initiator");
        }
        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::mapToRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getUserParticipationRequests(Long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::mapToRequestDto)
                .collect(Collectors.toList());
    }

    // === Helpers ===

    private Long getViews(Long eventId) {
        Event event = new Event();
        event.setId(eventId);

        Map<Long, Long> map = getViewsBatch(List.of(event));
        return map.getOrDefault(eventId, 0L);
    }

    private Map<Long, Long> getViewsBatch(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusYears(100);

        try {
            List<ViewStatsDto> stats = statClient.getStats(start, end, uris, true);
            Map<Long, Long> result = new HashMap<>();
            for (ViewStatsDto dto : stats) {
                String uri = dto.getUri();
                try {
                    Long id = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                    result.put(id, dto.getHits());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse event ID from URI: {}", uri);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error calling Stat Client", e);
            return Collections.emptyMap();
        }
    }

    private void updateEventFields(Event event, String annotation, Long categoryId,
                                   String description, LocalDateTime eventDate,
                                   ru.practicum.location.dto.Location location, Boolean paid,
                                   Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null && !annotation.isBlank()) event.setAnnotation(annotation);
        if (categoryId != null) event.setCategory(checkCategoryExists(categoryId));
        if (description != null && !description.isBlank()) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (location != null)
            event.setLocation(new ru.practicum.location.model.LocationEntity(location.getLat(), location.getLon()));
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null && !title.isBlank()) event.setTitle(title);
    }

    private User checkUserExists(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    }

    private Category checkCategoryExists(Long catId) {
        return categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException("Category " + catId + " not found"));
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
    }

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new BadRequestException("Event date too early");
        }
    }

    private Map<Long, Long> getEventsViews(List<Event> events) {
        Map<Long, Long> views = new HashMap<>();

        if (events == null || events.isEmpty()) {
            return views;
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statClient.getStats(
                    LocalDateTime.now().minusYears(10),
                    LocalDateTime.now().plusYears(1),
                    uris,
                    true);

            for (ViewStatsDto stat : stats) {
                try {
                    String uri = stat.getUri();
                    Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                    views.put(eventId, stat.getHits());
                } catch (Exception e) {
                    log.warn("Ошибка парсинга URI из статистики: {}", stat.getUri());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики от stats-server: {}", e.getMessage());
        }

        return views;
    }

    private Long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
    }
}
