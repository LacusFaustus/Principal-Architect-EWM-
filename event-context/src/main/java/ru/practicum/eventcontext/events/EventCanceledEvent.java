package ru.practicum.eventcontext.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Событие: событие отменено
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventCanceledEvent {
    private String eventId;
    private String reason;
    private LocalDateTime canceledAt;
}