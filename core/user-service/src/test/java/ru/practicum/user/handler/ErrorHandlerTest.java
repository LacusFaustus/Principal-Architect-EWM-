package ru.practicum.user.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.user.exception.BadRequestException;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.exception.NotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerTest {

    private ErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new ErrorHandler();
    }

    @Test
    void handleNotFound_ShouldReturnApiError() {
        NotFoundException exception = new NotFoundException("User not found");

        ApiError result = errorHandler.handleNotFound(exception);

        assertNotNull(result);
        assertEquals("User not found", result.getMessage());
        assertEquals("NOT_FOUND", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }

    @Test
    void handleConflict_ShouldReturnApiError() {
        ConflictException exception = new ConflictException("User already exists");

        ApiError result = errorHandler.handleConflict(exception);

        assertNotNull(result);
        assertEquals("User already exists", result.getMessage());
        assertEquals("CONFLICT", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }

    @Test
    void handleBadRequest_ShouldReturnApiError() {
        BadRequestException exception = new BadRequestException("Invalid request");

        ApiError result = errorHandler.handleBadRequest(exception);

        assertNotNull(result);
        assertEquals("Invalid request", result.getMessage());
        assertEquals("BAD_REQUEST", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }

    @Test
    void handleBadRequest_WithNullMessage_ShouldHandleGracefully() {
        // Используем исключение с null сообщением
        BadRequestException exception = new BadRequestException(null);

        ApiError result = errorHandler.handleBadRequest(exception);

        assertNotNull(result);
        // Проверяем, что message не null - используем значение по умолчанию
        assertNotNull(result.getMessage());
        // Проверяем, что сообщение содержит информацию об ошибке
        assertTrue(result.getMessage().contains("Incorrectly made request") ||
                result.getMessage().equals("Unknown error occurred") ||
                result.getMessage().equals("Incorrectly made request."));
        assertEquals("BAD_REQUEST", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }

    @Test
    void handleThrowable_ShouldReturnApiError() {
        RuntimeException exception = new RuntimeException("Internal error");

        ApiError result = errorHandler.handleThrowable(exception);

        assertNotNull(result);
        assertEquals("Internal error", result.getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void handleThrowable_WithNullMessage_ShouldHandleGracefully() {
        // Используем исключение с null сообщением
        RuntimeException exception = new RuntimeException((String) null);

        ApiError result = errorHandler.handleThrowable(exception);

        assertNotNull(result);
        // Проверяем, что message не null
        assertNotNull(result.getMessage());
        // Проверяем, что сообщение содержит информацию об ошибке
        assertTrue(result.getMessage().contains("Error occurred") ||
                result.getMessage().equals("Unknown error occurred") ||
                result.getMessage().equals("Error occurred"));
        assertEquals("INTERNAL_SERVER_ERROR", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }

    @Test
    void handleBadRequest_WithEmptyMessage_ShouldHandleGracefully() {
        BadRequestException exception = new BadRequestException("");

        ApiError result = errorHandler.handleBadRequest(exception);

        assertNotNull(result);
        assertNotNull(result.getMessage());
        assertEquals("BAD_REQUEST", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }

    @Test
    void handleThrowable_WithEmptyMessage_ShouldHandleGracefully() {
        RuntimeException exception = new RuntimeException("");

        ApiError result = errorHandler.handleThrowable(exception);

        assertNotNull(result);
        assertNotNull(result.getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", result.getStatus());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getReason());
        assertNotNull(result.getErrors());
    }
}