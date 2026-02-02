package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.*;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    // Получение событий пользователя
    List<EventShortDto> getEvents(Long userId, Pageable pageable);

    // Создание события пользователем
    EventFullDto postEvent(Long userId, NewEventDto newEventDto);

    // Получение конкретного события пользователя
    EventFullDto getEvent(Long userId, Long eventId);

    // Обновление события пользователем
    EventFullDto patchEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    // Получение событий по фильтрам администратором
    List<EventFullDto> getEventsByAdminFilters(EventParams params);

    // Обновление события администратором
    EventFullDto patchEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    // Получение событий по публичным фильтрам
    List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request);

    // Получение события по ID для публичного доступа
    EventFullDto getEventById(Long eventId, HttpServletRequest request);

    // Создание заявки на участие
    ParticipationRequestDto postRequest(Long userId, Long eventId);

    // Отмена заявки на участие пользователем
    ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId);

    // Обновление статуса заявок на участие (для инициатора события)
    EventRequestStatusUpdateResult updateEventRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest updateRequest);

    // Получение заявок на участие в событии пользователя
    List<ParticipationRequestDto> getEventParticipationRequests(Long userId, Long eventId);

    // Получение заявок на участие пользователя
    List<ParticipationRequestDto> getUserParticipationRequests(Long userId);

    // Сохранение статистики
    void saveStats(HttpServletRequest request);
}
