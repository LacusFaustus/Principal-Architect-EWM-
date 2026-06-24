package ru.practicum.event.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventUpdatedEvent {
    private String eventId;
    private String newTitle;
    private String newAnnotation;
    private String newDescription;
    private LocalDateTime updatedAt;
}