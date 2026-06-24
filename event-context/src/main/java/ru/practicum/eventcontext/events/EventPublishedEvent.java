package ru.practicum.eventcontext.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Событие: событие опубликовано
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventPublishedEvent {
    private String eventId;
    private LocalDateTime publishedAt;
}