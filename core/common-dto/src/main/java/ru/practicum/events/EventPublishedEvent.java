package ru.practicum.events;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EventPublishedEvent {
    private Long eventId;
    private LocalDateTime publishedAt;
    private String state;
}