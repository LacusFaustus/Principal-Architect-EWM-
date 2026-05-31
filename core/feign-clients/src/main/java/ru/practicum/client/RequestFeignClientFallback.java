package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RequestFeignClientFallback implements RequestFeignClient {

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.warn("Fallback: Request service unavailable for getUserRequests({})", userId);
        return Collections.emptyList();
    }

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.warn("Fallback: Request service unavailable for createRequest({}, {})", userId, eventId);
        ParticipationRequestDto fallback = new ParticipationRequestDto();
        fallback.setId(-1L);
        fallback.setStatus("PENDING");
        return fallback;
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.warn("Fallback: Request service unavailable for cancelRequest({}, {})", userId, requestId);
        ParticipationRequestDto fallback = new ParticipationRequestDto();
        fallback.setId(requestId);
        fallback.setStatus("CANCELED");
        return fallback;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.warn("Fallback: Request service unavailable for getEventRequests({}, {})", userId, eventId);
        return Collections.emptyList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.warn("Fallback: Request service unavailable for updateRequestStatus({}, {})", userId, eventId);
        return new EventRequestStatusUpdateResult();
    }

    @Override
    public Long countConfirmedRequestsByEventId(Long eventId) {
        log.warn("Fallback: Request service unavailable for countConfirmedRequestsByEventId({})", eventId);
        return 0L;
    }
}