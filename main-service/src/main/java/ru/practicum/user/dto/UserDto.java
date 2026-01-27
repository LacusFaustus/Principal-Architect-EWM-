package ru.practicum.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private String email;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    private String name;
}
