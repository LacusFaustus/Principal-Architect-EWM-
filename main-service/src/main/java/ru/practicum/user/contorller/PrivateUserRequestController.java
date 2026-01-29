package ru.practicum.user.contorller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.ParticipationRequestDto;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}/requests")
public class PrivateUserRequestController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {
        ParticipationRequestDto request = eventService.postRequest(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(@PathVariable Long userId) {
        // TODO: временная заглушка
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        // Временная реализация
        ParticipationRequestDto request = new ParticipationRequestDto();
        request.setId(requestId);
        request.setRequester(userId);
        request.setEvent(1L); // временное значение
        request.setStatus("CANCELED");
        request.setCreated(java.time.LocalDateTime.now());
        return ResponseEntity.ok(request);

    }
}
