package ru.practicum.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCredentialsDto {
    private Long userId;
    private String email;
    private String password;
    private String role;
}