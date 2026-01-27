package ru.practicum.handler;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, Integer status, String error, String message) {
}
