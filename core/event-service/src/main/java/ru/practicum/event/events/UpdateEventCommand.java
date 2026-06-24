package ru.practicum.event.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventCommand {
    private String id;
    private String newTitle;
    private String newAnnotation;
    private String newDescription;
}