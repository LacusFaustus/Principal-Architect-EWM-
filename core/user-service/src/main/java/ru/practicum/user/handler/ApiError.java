package ru.practicum.user.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class ApiError {
    private List<String> errors;
    private String message;
    private String reason;
    private String status;
    private String timestamp;

    public ApiError() {
        this.errors = List.of();
        this.message = "Unknown error occurred";
        this.reason = "An error occurred";
        this.status = "INTERNAL_SERVER_ERROR";
        this.timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }

    public ApiError(List<String> errors, String message, String reason, String status) {
        this.errors = errors != null ? errors : List.of();
        this.message = (message != null && !message.isEmpty()) ? message : "Unknown error occurred";
        this.reason = reason != null ? reason : "An error occurred";
        this.status = status != null ? status : "INTERNAL_SERVER_ERROR";
        this.timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }
}