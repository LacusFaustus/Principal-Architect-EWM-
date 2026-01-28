package ru.practicum.user.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "empty or null email")
    @Email(message = "invalid email")
    @Size(min = 6, max = 254)
    private String email;
    @NotBlank(message = "empty or null name")
    @Size(min = 2, max = 250)
    private String name;
}
