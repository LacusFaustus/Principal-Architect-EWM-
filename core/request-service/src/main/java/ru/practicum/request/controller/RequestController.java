package ru.practicum.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests", userId);
        return ResponseEntity.ok(requestService.getRequests(userId));
    }

    @PostMapping("/users/{userId}/requests")
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {
        log.info("POST /users/{}/requests?eventId={}", userId, eventId);
        ParticipationRequestDto request = requestService.postRequest(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        ParticipationRequestDto request = requestService.patchRequest(userId, requestId);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return ResponseEntity.ok(requestService.getEventRequests(userId, eventId));
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> updateRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}/requests", userId, eventId);
        EventRequestStatusUpdateResult result = requestService.patchEventRequestsStatus(userId, eventId, updateRequest);
        return ResponseEntity.ok(result);
    }

    // Internal API для других микросервисов
    @GetMapping("/events/{eventId}/requests/count")
    public ResponseEntity<Long> countConfirmedRequestsByEventId(@PathVariable Long eventId) {
        log.info("GET /events/{}/requests/count (internal)", eventId);
        return ResponseEntity.ok(requestService.countConfirmedRequestsByEventId(eventId));
    }
}