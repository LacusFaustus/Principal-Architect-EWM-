package ru.practicum.compilations.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompilationSearchParam {
    private Boolean pinned;
    @PositiveOrZero
    private int from = 0;
    @PositiveOrZero
    private int size = 10;
}
