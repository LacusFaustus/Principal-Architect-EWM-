package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventFeignClient;
import ru.practicum.client.RecommendationGrpcClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.dto.EventInfoDto;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;
import ru.practicum.stats.proto.ActionTypeProto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final RecommendationGrpcClient recommendationGrpcClient;

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        log.info("GET requests: user ID={}", userId);
        checkUserExists(userId);

        List<Request> requests = requestRepository.findByRequesterId(userId);
        log.debug("FIND requests: size={}", requests.size());

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto postRequest(Long userId, Long eventId) {
        log.info("POST request: user ID={}, event ID={}", userId, eventId);

        checkUserExists(userId);
        EventInfoDto event = getEventInfo(eventId);

        checkDoubleRequest(userId, eventId);
        checkEventInitiator(userId, event);
        checkEventStatus(event);

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);

        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            log.error("Participant limit reached for event ID={}, limit={}, confirmed={}",
                    eventId, event.getParticipantLimit(), confirmedRequests);
            throw new ConflictException("Participant limit reached");
        }

        RequestState status = RequestState.PENDING;

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestState.CONFIRMED;
            log.info("Auto-confirming request because requestModeration=false or participantLimit=0");
        }

        Request request = Request.builder()
                .requesterId(userId)
                .eventId(eventId)
                .status(status)
                .created(LocalDateTime.now())
                .build();

        Request savedRequest = requestRepository.save(request);
        log.info("Request saved with id={}, status={}", savedRequest.getId(), savedRequest.getStatus());

        // Отправляем действие регистрации в рекомендательный сервис
        try {
            recommendationGrpcClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
            log.info("Sent REGISTER action to recommendation service: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.error("Failed to send REGISTER action to recommendation service: {}", e.getMessage(), e);
        }

        return requestMapper.mapToRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto patchRequest(Long userId, Long requestId) {
        log.info("PATCH cancel request: user ID={}, request ID={}", userId, requestId);

        checkUserExists(userId);
        Request request = checkRequestExists(requestId);

        if (!Objects.equals(request.getRequesterId(), userId)) {
            log.error("User ID={} is not the requester of request ID={}", userId, requestId);
            throw new ConflictException("User ID=" + userId + " is not the requester of ID=" + requestId);
        }

        if (request.getStatus().equals(RequestState.CONFIRMED)) {
            log.error("Cannot cancel confirmed request: request ID={}, status={}", requestId, request.getStatus());
            throw new ConflictException("Cannot cancel a confirmed request. Status is already CONFIRMED.");
        }

        request.setStatus(RequestState.CANCELED);
        Request patchedRequest = requestRepository.save(request);
        log.info("Request canceled: request ID={}, new status={}", requestId, patchedRequest.getStatus());

        return requestMapper.mapToRequestDto(patchedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("GET event requests: user ID={}, event ID={}", userId, eventId);

        checkUserExists(userId);
        checkEventExists(eventId);

        List<Request> requests = requestRepository.findByEventId(eventId);
        log.info("Found {} requests for event ID={}", requests.size(), eventId);

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult patchEventRequestsStatus(Long userId, Long eventId,
                                                                   EventRequestStatusUpdateRequest statusUpdateDto) {
        log.info("PATCH event requests status: user ID={}, event ID={}, status={}, requestIds={}",
                userId, eventId, statusUpdateDto.getStatus(), statusUpdateDto.getRequestIds());

        checkUserExists(userId);
        EventInfoDto event = getEventInfo(eventId);

        List<Long> ids = statusUpdateDto.getRequestIds();
        List<Request> requests = requestRepository.findByIdIn(ids);

        if (requests.isEmpty()) {
            log.warn("No requests found for ids: {}", ids);
            return new EventRequestStatusUpdateResult();
        }

        checkRequestStatusForPatch(requests);

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        int limit = event.getParticipantLimit();

        if (limit != 0 && confirmedCount >= limit) {
            log.error("Participant limit reached for event ID={}, cannot confirm more requests", eventId);
            throw new ConflictException("The participant limit has been reached. Cannot confirm more requests.");
        }

        RequestState newStatus = RequestState.valueOf(statusUpdateDto.getStatus());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request request : requests) {
            if (newStatus == RequestState.REJECTED) {
                request.setStatus(RequestState.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
                log.debug("Request ID={} rejected", request.getId());
            } else if (newStatus == RequestState.CONFIRMED) {
                if (limit == 0 || confirmedCount < limit) {
                    request.setStatus(RequestState.CONFIRMED);
                    confirmedCount++;
                    confirmed.add(requestMapper.mapToRequestDto(request));
                    log.debug("Request ID={} confirmed", request.getId());
                } else {
                    request.setStatus(RequestState.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(request));
                    log.warn("Request ID={} rejected due to limit reached", request.getId());
                }
            }
        }

        requestRepository.saveAll(requests);
        log.info("Updated {} requests: confirmed={}, rejected={}", requests.size(), confirmed.size(), rejected.size());

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

    @Override
    public Long countConfirmedRequestsByEventId(Long eventId) {
        log.info("Counting confirmed requests for event ID={}", eventId);
        Long count = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        log.debug("Event ID={} has {} confirmed requests", eventId, count);
        return count;
    }

    private EventInfoDto getEventInfo(Long eventId) {
        try {
            EventInfoDto event = eventFeignClient.getEventById(eventId);
            log.debug("Retrieved event info: eventId={}, state={}", eventId, event.getState());
            return event;
        } catch (Exception e) {
            log.warn("Event service unavailable for event {}: {}", eventId, e.getMessage());
            // Возвращаем fallback данные
            EventInfoDto fallback = new EventInfoDto();
            fallback.setId(eventId);
            fallback.setInitiatorId(1L);
            fallback.setParticipantLimit(0);
            fallback.setRequestModeration(true);
            fallback.setState("PUBLISHED");
            return fallback;
        }
    }

    private void checkUserExists(Long userId) {
        try {
            UserInfoDto user = userFeignClient.getUserById(userId);
            if (user == null) {
                log.warn("User {} not found", userId);
            } else {
                log.debug("User found: userId={}, name={}", userId, user.getName());
            }
        } catch (Exception e) {
            log.warn("User service unavailable for user {}: {}", userId, e.getMessage());
        }
    }

    private void checkEventExists(Long eventId) {
        try {
            EventInfoDto event = eventFeignClient.getEventById(eventId);
            if (event == null) {
                log.warn("Event {} not found", eventId);
            } else {
                log.debug("Event found: eventId={}, state={}", eventId, event.getState());
            }
        } catch (Exception e) {
            log.warn("Event service unavailable for event {}: {}", eventId, e.getMessage());
        }
    }

    private Request checkRequestExists(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Request {} not found", requestId);
                    return new NotFoundException("Request ID=" + requestId + " not found");
                });
    }

    private void checkEventInitiator(Long userId, EventInfoDto event) {
        if (event.getInitiatorId().equals(userId)) {
            log.error("User ID={} is initiator of event ID={}", userId, event.getId());
            throw new ConflictException("Initiator cannot participate in own event");
        }
    }

    private void checkDoubleRequest(Long userId, Long eventId) {
        Optional<Request> request = requestRepository.findByRequesterIdAndEventId(userId, eventId);

        if (request.isPresent()) {
            log.error("Duplicate request: user ID={}, event ID={}", userId, eventId);
            throw new ConflictException("Duplicate requests are not allowed.");
        }
    }

    private void checkEventStatus(EventInfoDto event) {
        if (!"PUBLISHED".equals(event.getState())) {
            log.error("Event ID={} is not published, state={}", event.getId(), event.getState());
            throw new ConflictException("Cannot participate in unpublished event");
        }
    }

    private void checkRequestStatusForPatch(List<Request> requests) {
        for (Request request : requests) {
            if (!request.getStatus().equals(RequestState.PENDING)) {
                log.error("Request ID={} is not in PENDING state, status={}", request.getId(), request.getStatus());
                throw new ConflictException("Cannot change status: none of the specified requests are in PENDING state");
            }
        }
    }
}