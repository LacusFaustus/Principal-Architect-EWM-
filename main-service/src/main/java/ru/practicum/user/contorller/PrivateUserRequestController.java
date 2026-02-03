package ru.practicum.user.contorller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.service.RequestService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}/requests")
public class PrivateUserRequestController {
    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<ParticipationRequestDto> postRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.postRequest(userId, eventId));
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getUserRequests(
            @PathVariable Long userId) {

        return ResponseEntity.ok()
                .body(requestService.getRequests(userId));
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<ParticipationRequestDto> cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {

        return ResponseEntity.ok()
                .body(requestService.patchRequest(userId, requestId));
    }
}