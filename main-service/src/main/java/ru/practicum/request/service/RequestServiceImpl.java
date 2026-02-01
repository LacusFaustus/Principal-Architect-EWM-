package ru.practicum.request.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.NewRequestDto;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

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

        User user = checkUserExists(userId);
        Event event = checkEventExists(eventId);
        checkDoubleRequest(userId, eventId);

        RequestState requestState = RequestState.PENDING;

        if (event.getParticipantLimit() == 0) {
            requestState = RequestState.CONFIRMED;
            checkEventStatus(event);
        } else {
            checkConflictRequest(event, user);
        }

        NewRequestDto newRequestDto = new NewRequestDto(user, event, requestState, LocalDateTime.now());
        Request request = requestMapper.mapToRequest(newRequestDto);
        log.debug("MAP request: {}", request);

        Request savedRequest = requestRepository.save(request);
        log.debug("SAVED request: {}", savedRequest);

        return requestMapper.mapToRequestDto(savedRequest);
    }

    @Override
    @Transactional
    public ParticipationRequestDto patchRequest(Long userId, Long requestId) {
        log.info("PATCH cancel request ID={}", requestId);

        checkUserExists(userId);
        Request request = checkRequestExists(requestId);
        checkConflictCancelRequest(request, userId);

        request.setStatus(RequestState.CANCELED);
        Request patchedRequest = requestRepository.save(request);
        log.info("PATCH request: {}", patchedRequest);

        return requestMapper.mapToRequestDto(request);
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("GET event ID={} requests", eventId);

        checkUserExists(userId);
        checkEventExists(eventId);

        List<Request> requests = requestRepository.findByEventId(eventId);
        log.info("FIND requests size={} requests", requests.size());

        return requests.stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult patchEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateDto) {
        log.info("PATCH status requests ID={}", statusUpdateDto.getRequestIds());

        checkUserExists(userId);
        Event event = checkEventExists(eventId);
        List<Long> ids = statusUpdateDto.getRequestIds().stream()
                .map(Integer::longValue)
                .toList();
        List<Request> requests = requestRepository.findByIdIn(ids);

        if (requests.isEmpty()) {
            return new EventRequestStatusUpdateResult();
        }

        checkRequestStatusForPatch(requests);

        Integer eventLimit = event.getParticipantLimit();
        Long eventConfirmRequests = checkEventLimit(event);
        RequestState newStatus = RequestState.valueOf(statusUpdateDto.getStatus());

        if (newStatus.equals(RequestState.REJECTED)) {
            requests.forEach(request -> request.setStatus(newStatus));
        }

        if (newStatus.equals(RequestState.CONFIRMED)) {
            requests.forEach(request -> request.setStatus(newStatus));

            if (event.getParticipantLimit() == 0) {
                requests.forEach(request -> request.setStatus(newStatus));
            } else {
                long availableSlots = eventLimit - eventConfirmRequests;

                for (Request request : requests) {
                    if (availableSlots > 0) {
                        request.setStatus(newStatus);
                        availableSlots--;
                    } else {
                        request.setStatus(RequestState.REJECTED);
                    }
                }
            }
        }

        List<ParticipationRequestDto> updatedRequests = requestRepository.saveAll(requests).stream()
                .map(requestMapper::mapToRequestDto)
                .toList();
        log.info("UPDATED status requests ID={}", statusUpdateDto.getRequestIds());

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (ParticipationRequestDto requestDto : updatedRequests) {
            if (requestDto.getStatus().equals("CONFIRMED")) {
                confirmedRequests.add(requestDto);
            }

            if (requestDto.getStatus().equals("REJECTED")) {
                rejectedRequests.add(requestDto);
            }
        }

        result.setConfirmedRequests(confirmedRequests);
        result.setRejectedRequests(rejectedRequests);

        return result;
    }

    private User checkUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found", userId);
                    return new NotFoundException("User ID=" + userId + " not found");
                });
    }

    private Request checkRequestExists(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Request {} not found", requestId);
                    return new NotFoundException("Request ID=" + requestId + " not found");
                });
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("Event {} not found", eventId);
                    return new NotFoundException("Event ID=" + eventId + " not found");
                });
    }

    private void checkConflictCancelRequest(Request request, Long userId) {
        if (!Objects.equals(request.getRequester().getId(), userId)) {
            log.error("User ID={} cannot canceled Request ID={}", userId, request.getId());
            throw new ConflictException("This user cannot canceled request");
        }
    }

    private void checkConflictRequest(Event event, User user) {
        checkEventInitiator(user.getId(), event);
        checkEventStatus(event);
        checkEventLimit(event);
    }

    private void checkEventInitiator(Long userId, Event event) {
        if (event.getInitiator().getId().equals(userId)) {
            log.error("User ID={} initiator event ID={}", userId, event.getId());
            throw new ConflictException("Initiator cannot participate in own event");
        }
    }

    private void checkDoubleRequest(Long userId, Long eventId) {
        Optional<Request> request = requestRepository.findByRequesterIdAndEventId(userId, eventId);

        if (request.isPresent()) {
            log.error("Try double request user ID={}, for event ID={}=", userId, eventId);
            throw new ConflictException("Duplicate requests are not allowed.");
        }
    }

    private Long checkEventLimit(Event event) {
        Integer eventLimit = event.getParticipantLimit();
        Long eventConfirmRequests = requestRepository.countByEventIdAndStatusIn(event.getId(),
                List.of(RequestState.PENDING, RequestState.CONFIRMED));

        if (eventLimit - eventConfirmRequests == 0) {
            log.error("Participant limit reached event ID={}", event.getId());
            throw new ConflictException("Participant limit reached");
        }

        return eventConfirmRequests;
    }

    private void checkEventStatus(Event event) {
        if (event.getState() != EventState.PUBLISHED) {
            log.error("Event ID={} unpublished", event.getId());
            throw new ConflictException("Cannot participate in unpublished event");
        }
    }

    private void checkRequestStatusForPatch(List<Request> requests) {
        for (Request request : requests) {
            if (!request.getStatus().equals(RequestState.PENDING)) {
                log.error("Request ID={} none of the specified requests are in PENDING state", request.getId());
                throw new ConflictException("Cannot change status: none of the specified requests are in PENDING state");
            }
        }
    }
}

