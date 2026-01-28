package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.event.dto.*;

import java.util.List;

public interface EventService {
    // Создание события пользователем
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    // Получение событий пользователя
    List<EventShortDto> getUserEvents(Long userId, PageParams pageParams);

    // Получение конкретного события пользователя
    EventFullDto getUserEvent(Long userId, Long eventId);

    // Обновление события пользователем
    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    // Получение событий по фильтрам администратором
    List<EventFullDto> getEventsByAdminFilters(EventParams params);

    // Обновление события администратором
    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    // Получение событий по публичным фильтрам
    List<EventShortDto> getEventsByPublicFilters(PublicEventParams params, HttpServletRequest request);

    // Получение события по ID для публичного доступа
    EventFullDto getEventById(Long eventId, HttpServletRequest request);

    // Создание заявки на участие
    ParticipationRequestDto createParticipationRequest(Long userId, Long eventId);

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
