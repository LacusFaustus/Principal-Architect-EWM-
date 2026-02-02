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
        LocalDateTime start = params.getRangeStart();
        LocalDateTime end = params.getRangeEnd();

        if (start == null && end == null) {
            start = LocalDateTime.now();
        }

        String text = (params.getText() != null && !params.getText().isBlank())
                ? params.getText() : null;

        int pageNum = params.getPageParams().getFrom() / params.getPageParams().getSize();
        Pageable pageable = PageRequest.of(pageNum, params.getPageParams().getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate"));

        if ("VIEWS".equals(params.getSort())) {
            return getEventsSortedByViews(params, start);
        }

        try {
            Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                    text,
                    params.getCategories(),
                    params.getPaid(),
                    start,
                    end,
                    pageable);

            List<Event> events = eventsPage.getContent();
            Map<Long, Long> viewsMap = getEventsViews(events);

            return events.stream()
                    .map(event -> {
                        Long confirmed = getConfirmedRequests(event.getId());
                        return eventMapper.toEventShortDto(event, viewsMap.getOrDefault(event.getId(), 0L), confirmed);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in public filters: {}", e.getMessage());
            throw e;
        }
    }

    private List<EventShortDto> getEventsSortedByViews(PublicEventParams params, LocalDateTime rangeStart) {
        Pageable allRecords = PageRequest.of(0, Integer.MAX_VALUE);

        Page<Event> eventsPage = eventRepository.findEventsByPublicFilters(
                params.getText(), params.getCategories(), params.getPaid(),
                rangeStart, params.getRangeEnd(), allRecords);

        List<Event> events = eventsPage.getContent();
        Map<Long, Long> viewsMap = getEventsViews(events);

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

        return eventMapper.toEventFullDto(event, getViews(eventId),
                requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED));
    }

    @Override
    public void saveStats(HttpServletRequest request) {
        try {
            statClient.saveHit(new NewEndpointHitDto(
                    "main-service",
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage());
        }
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
        if (events.isEmpty()) return Collections.emptyMap();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStatsDto> stats = statClient.getStats(
                    LocalDateTime.now().minusYears(10), // Берем большой интервал
                    LocalDateTime.now().plusYears(1),
                    uris,
                    true);

            return stats.stream()
                    .filter(s -> s.getUri().contains("/events/"))
                    .collect(Collectors.toMap(
                            s -> Long.parseLong(s.getUri().substring(s.getUri().lastIndexOf("/") + 1)),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            return Collections.emptyMap(); // Возвращаем пустую карту вместо ошибки
        }
    }

    private Long getConfirmedRequests(Long eventId) {
        try {
            return requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        } catch (Exception e) {
            log.error("Ошибка при подсчете заявок для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }
}
