package ru.practicum.compilations.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompilationSearchParam {
    private Boolean pinned;

    @PositiveOrZero
    private int from = 0;

    @PositiveOrZero
    private int size = 10;
}
