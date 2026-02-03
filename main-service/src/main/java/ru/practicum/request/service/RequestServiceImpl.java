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
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
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
        User user = checkUserExists(userId);
        Event event = checkEventExists(eventId);

        checkDoubleRequest(userId, eventId);
        checkEventInitiator(userId, event);
        checkEventStatus(event);

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);

        if (event.getParticipantLimit() != 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit reached");
        }

        RequestState status = RequestState.PENDING;

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestState.CONFIRMED;
        }

        Request request = Request.builder()
                .requester(user)
                .event(event)
                .status(status)
                .created(LocalDateTime.now())
                .build();

        return requestMapper.mapToRequestDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto patchRequest(Long userId, Long requestId) {
        log.info("PATCH cancel request ID={} by user ID={}", requestId, userId);

        checkUserExists(userId);
        Request request = checkRequestExists(requestId);

        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new ConflictException("User ID=" + userId + " is not the requester of ID=" + requestId);
        }

        if (request.getStatus().equals(RequestState.CONFIRMED)) {
            throw new ConflictException("Cannot cancel a confirmed request. Status is already CONFIRMED.");
        }

        request.setStatus(RequestState.CANCELED);
        Request patchedRequest = requestRepository.save(request);

        return requestMapper.mapToRequestDto(patchedRequest);
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

    @Override
    @Transactional
    public EventRequestStatusUpdateResult patchEventRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest statusUpdateDto) {
        checkUserExists(userId);
        Event event = checkEventExists(eventId);

        List<Long> ids = statusUpdateDto.getRequestIds();
        List<Request> requests = requestRepository.findByIdIn(ids);

        if (requests.isEmpty()) {
            return new EventRequestStatusUpdateResult();
        }

        checkRequestStatusForPatch(requests);

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED);
        int limit = event.getParticipantLimit();

        if (limit != 0 && confirmedCount >= limit) {
            throw new ConflictException("The participant limit has been reached. Cannot confirm more requests.");
        }

        RequestState newStatus = RequestState.valueOf(statusUpdateDto.getStatus());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (Request request : requests) {
            if (newStatus == RequestState.REJECTED) {
                request.setStatus(RequestState.REJECTED);
                rejected.add(requestMapper.mapToRequestDto(request));
            } else if (newStatus == RequestState.CONFIRMED) {

                if (limit == 0 || confirmedCount < limit) {
                    request.setStatus(RequestState.CONFIRMED);
                    confirmedCount++;
                    confirmed.add(requestMapper.mapToRequestDto(request));
                } else {

                    request.setStatus(RequestState.REJECTED);
                    rejected.add(requestMapper.mapToRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

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