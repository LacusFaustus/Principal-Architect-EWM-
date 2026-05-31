package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.EventInfoDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.NotFoundException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventInfoDto> getEventById(@PathVariable Long eventId) {
        log.info("Internal API: GET event by ID={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        EventInfoDto eventInfo = new EventInfoDto();
        eventInfo.setId(event.getId());
        eventInfo.setInitiatorId(event.getInitiatorId());
        eventInfo.setParticipantLimit(event.getParticipantLimit());
        eventInfo.setRequestModeration(event.getRequestModeration());
        eventInfo.setState(event.getState().name());

        return ResponseEntity.ok(eventInfo);
    }
}