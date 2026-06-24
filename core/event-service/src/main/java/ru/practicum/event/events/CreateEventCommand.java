package ru.practicum.event.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventCommand {
    private String id;
    private String title;
    private String annotation;
    private String description;
    private String category;
    private Long initiatorId;
    private Integer participantLimit;
    private Boolean requestModeration;
    private Boolean paid;
    private LocalDateTime eventDate;
}