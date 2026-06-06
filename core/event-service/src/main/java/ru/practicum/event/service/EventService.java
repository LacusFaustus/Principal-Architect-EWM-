package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.*;

import java.util.List;

public interface EventService {
    List<EventShortDto> getEvents(Long userId, Pageable pageable);

    EventFullDto postEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEvent(Long userId, Long eventId);

    EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventFullDto> getEventsByAdminFilters(EventParams params);

    EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, Long userId);

    EventFullDto getEventById(Long eventId, Long userId, jakarta.servlet.http.HttpServletRequest request);

    List<EventShortDto> getRecommendationsForUser(Long userId, int size);

    void likeEvent(Long userId, Long eventId);

    boolean hasUserInteractedWithEvent(Long userId, Long eventId);

    void saveStats(HttpServletRequest request);
}