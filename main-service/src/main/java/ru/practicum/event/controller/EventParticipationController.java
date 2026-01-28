package ru.practicum.event.controller;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.event.dto.EventRequestStatusUpdateResult;
import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/requests")
public class EventParticipationController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) throws BadRequestException {
        try {
            // TODO: временная заглушка
            return ResponseEntity.ok(List.of());
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @PatchMapping
    public ResponseEntity<EventRequestStatusUpdateResult> updateEventRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest updateRequest) throws BadRequestException {
        try {
            // Временная заглушка
            EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
            return ResponseEntity.ok(result);
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }
}
