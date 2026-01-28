package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.PageParams;
import ru.practicum.event.dto.PublicEventParams;
import ru.practicum.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/events")
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEventsByPublicFilters(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(name = "from", defaultValue = "0") Integer from,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            HttpServletRequest request) throws BadRequestException {
        try {
            PageParams pageParams = new PageParams(from, size);
            PublicEventParams params = new PublicEventParams(text, categories, paid, rangeStart,
                    rangeEnd, onlyAvailable, sort, pageParams);

            // Сначала получаем данные
            List<EventShortDto> events = eventService.getEventsByPublicFilters(params, request);

            // Потом сохраняем статистику
            eventService.saveStats(request);

            return ResponseEntity.ok(events);
        } catch (RuntimeException ex) {
            log.error("Error in getEventsByPublicFilters: {}", ex.getMessage());
            throw new BadRequestException();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventById(@PathVariable Long id,
                                                     HttpServletRequest request) throws BadRequestException {
        try {
            EventFullDto event = eventService.getEventById(id, request);

            // Временно закомментируем
            // eventService.saveStats(request);

            return ResponseEntity.ok(event);
        } catch (RuntimeException ex) {
            // log.error("Error in getEventById for event {}: {}", id, ex.getMessage());
            throw new BadRequestException();
        }
    }
}
