package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.event.service.EventService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventFullDto> createEvent(@PathVariable Long userId,
                                                    @Valid @RequestBody NewEventDto newEventDto) throws BadRequestException {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(userId, newEventDto));
        } catch (RuntimeException ex) {
            // Просто выбрасываем исключение без логов
            throw new BadRequestException();
        }
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getUserEvents(@PathVariable Long userId,
                                                             @RequestParam(name = "from", defaultValue = "0") Integer from,
                                                             @RequestParam(name = "size", defaultValue = "10") Integer size) throws BadRequestException {
        try {
            PageParams pageParams = new PageParams(from, size);
            return ResponseEntity.ok(eventService.getUserEvents(userId, pageParams));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getUserEvent(@PathVariable Long userId,
                                                     @PathVariable Long eventId) throws BadRequestException {
        try {
            return ResponseEntity.ok(eventService.getUserEvent(userId, eventId));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventByUser(@PathVariable Long userId,
                                                          @PathVariable Long eventId,
                                                          @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) throws BadRequestException {
        try {
            return ResponseEntity.ok(eventService.updateEventByUser(userId, eventId, updateEventUserRequest));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }
}
