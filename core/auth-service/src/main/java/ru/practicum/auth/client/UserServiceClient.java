package ru.practicum.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Временно отключаем FeignClient, так как он использует несуществующие DTO
// @FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    // @PostMapping("/internal/users")
    // void createUserProfile(@RequestBody UserProfileDto profileDto);

    // @DeleteMapping("/internal/users/{userId}")
    // void deleteUserProfile(@PathVariable("userId") Long userId);
}