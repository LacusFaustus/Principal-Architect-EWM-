package ru.practicum.events;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EventCreatedEvent {
    private Long eventId;
    private Long initiatorId;
    private String title;
    private String category;
    private LocalDateTime eventDate;
    private LocalDateTime createdAt;
}