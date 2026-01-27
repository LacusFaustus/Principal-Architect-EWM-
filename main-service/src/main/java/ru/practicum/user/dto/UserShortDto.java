package ru.practicum.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserShortDto {
    private String email;
    private String name;
}
