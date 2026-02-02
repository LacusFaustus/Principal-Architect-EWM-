package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.*;

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

    // Сохранение статистики
    void saveStats(HttpServletRequest request);
}
