package ru.practicum.location.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LocationEntity {
    private Double lat;
    private Double lon;
}