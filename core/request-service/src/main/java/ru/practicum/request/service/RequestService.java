package ru.practicum.request.service;

import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    List<ParticipationRequestDto> getRequests(Long userId);

    ParticipationRequestDto postRequest(Long userId, Long eventId);

    ParticipationRequestDto patchRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult patchEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateDto);

    Long countConfirmedRequestsByEventId(Long eventId);
}