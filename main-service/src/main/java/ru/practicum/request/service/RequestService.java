package ru.practicum.request.service;

import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;

import java.util.List;

public interface RequestService {
    //Заявки текущего пользователя
    List<ParticipationRequestDto> getRequests(Long userId);

    //Создание новой заявки
    ParticipationRequestDto postRequest(Long userId, Long eventId);

    //Отмена заявки
    ParticipationRequestDto patchRequest(Long userId, Long requestId);

    //Получение заявок к событию пользователя
    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    //Обновление статуса заявок на участие в событии
    EventRequestStatusUpdateResult patchEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateDto);
}
