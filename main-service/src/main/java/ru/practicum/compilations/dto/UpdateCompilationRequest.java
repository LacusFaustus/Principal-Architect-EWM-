package ru.practicum.compilations.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequest {
    private List<Long> events;

    private Boolean pinned;

    @Size(min = 1, max = 50)
    private String title;
}
