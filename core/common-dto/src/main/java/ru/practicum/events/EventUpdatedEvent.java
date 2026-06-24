package ru.practicum.events;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EventUpdatedEvent {
    private Long eventId;
    private String oldTitle;
    private String newTitle;
    private LocalDateTime updatedAt;
}