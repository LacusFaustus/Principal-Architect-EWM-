package ru.practicum.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "Email cannot be empty or null")
    @Email(message = "Invalid email format")
    @Size(min = 6, max = 254, message = "Email must be between 6 and 254 characters")
    private String email;

    @NotBlank(message = "Name cannot be empty or null")
    @Size(min = 2, max = 250, message = "Name must be between 2 and 250 characters")
    private String name;
}