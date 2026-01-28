package ru.practicum.event.controller;

import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}/requests")
public class ParticipationController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) throws BadRequestException {
        try {
            ParticipationRequestDto request = eventService.createParticipationRequest(userId, eventId);
            return ResponseEntity.status(HttpStatus.CREATED).body(request);
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) throws BadRequestException {
        try {
            // TODO: временная заглушка
            return ResponseEntity.ok(List.of());
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) throws BadRequestException {
        try {
            // Временная реализация
            ParticipationRequestDto request = new ParticipationRequestDto();
            request.setId(requestId);
            request.setRequester(userId);
            request.setEvent(1L); // временное значение
            request.setStatus("CANCELED");
            request.setCreated(java.time.LocalDateTime.now());
            return ResponseEntity.ok(request);
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }
}
