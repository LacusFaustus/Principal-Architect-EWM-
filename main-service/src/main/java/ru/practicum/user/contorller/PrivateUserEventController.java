package ru.practicum.user.contorller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateUserEventController {
    private final EventService eventService;

    @GetMapping("/events")
    public ResponseEntity<List<EventShortDto>> getEvents(@PathVariable Long userId,
                                                         @RequestParam(name = "from", defaultValue = "0") Integer from,
                                                         @RequestParam(name = "size", defaultValue = "10") Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        return ResponseEntity.ok(eventService.getEvents(userId, pageable));
    }

    @PostMapping("/events")
    public ResponseEntity<EventFullDto> postEvent(@PathVariable Long userId,
                                                  @Valid @RequestBody NewEventDto newEventDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.postEvent(userId, newEventDto));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventFullDto> getEvent(@PathVariable Long userId,
                                                 @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(userId, eventId));
    }

    @PatchMapping("/events/{eventId}")
    public ResponseEntity<EventFullDto> patchEvent(@PathVariable Long userId,
                                                   @PathVariable Long eventId,
                                                   @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        return ResponseEntity.ok(eventService.patchEventByUser(userId, eventId, updateEventUserRequest));
    }

    @GetMapping("/events/{eventId}/requests")
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        // TODO: временная заглушка
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/events/{eventId}/requests")
    public ResponseEntity<EventRequestStatusUpdateResult> patchEventRequestsStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        // Временная заглушка
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        return ResponseEntity.ok(result);
    }
}
