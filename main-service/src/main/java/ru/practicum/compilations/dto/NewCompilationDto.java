package ru.practicum.compilations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    private List<Long> events = new ArrayList<>();

    private Boolean pinned = false;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}
