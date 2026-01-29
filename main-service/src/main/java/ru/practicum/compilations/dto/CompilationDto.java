package ru.practicum.compilations.dto;

import lombok.*;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto {
    private Long id;

    private Boolean pinned = false;
    private String title;
    private List<EventShortDto> events;
}
