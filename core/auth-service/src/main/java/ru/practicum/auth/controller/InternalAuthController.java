package ru.practicum.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.auth.service.AuthService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final AuthService authService;

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        log.debug("Internal token validation request");
        return ResponseEntity.ok(authService.validateToken(token));
    }

    @PostMapping("/create")
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest request) {
        log.info("Internal: Creating user credentials for userId: {}", request.getUserId());
        authService.createUser(request.getUserId(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("Internal: Deleting user credentials for userId: {}", userId);
        // TODO: Реализовать удаление пользователя
        return ResponseEntity.ok().build();
    }

    // Внутренний DTO
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CreateUserRequest {
        private Long userId;
        private String email;
        private String password;
        private String role;
    }
}