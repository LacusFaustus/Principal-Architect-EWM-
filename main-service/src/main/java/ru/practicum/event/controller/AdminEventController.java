package ru.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventParams;
import ru.practicum.event.dto.PageParams;
import ru.practicum.event.dto.UpdateEventAdminRequest;
import ru.practicum.event.model.EventState;
import ru.practicum.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getEventsByAdminFilters(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(name = "from", defaultValue = "0") Integer from,
            @RequestParam(name = "size", defaultValue = "10") Integer size) throws BadRequestException {
        try {
            PageParams pageParams = new PageParams(from, size);
            EventParams params = new EventParams(users, states, categories, rangeStart, rangeEnd, pageParams);
            return ResponseEntity.ok(eventService.getEventsByAdminFilters(params));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventByAdmin(@PathVariable Long eventId,
                                                           @Valid @RequestBody UpdateEventAdminRequest updateEventAdminRequest) throws BadRequestException {
        try {
            return ResponseEntity.ok(eventService.updateEventByAdmin(eventId, updateEventAdminRequest));
        } catch (RuntimeException ex) {
            throw new BadRequestException();
        }
    }
}
