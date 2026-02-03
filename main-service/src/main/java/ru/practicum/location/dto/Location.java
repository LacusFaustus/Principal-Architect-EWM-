package ru.practicum.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    @NotNull
    private Double lat;
    @NotNull
    private Double lon;
}