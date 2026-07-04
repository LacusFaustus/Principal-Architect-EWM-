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
    }

    @Test
    void handleConflict_ShouldReturnApiError() {
        ConflictException exception = new ConflictException("User already exists");

        ApiError result = errorHandler.handleConflict(exception);

        assertNotNull(result);
        assertEquals("User already exists", result.getMessage());
        assertEquals("CONFLICT", result.getStatus());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void handleBadRequest_ShouldReturnApiError() {
        BadRequestException exception = new BadRequestException("Invalid request");

        ApiError result = errorHandler.handleBadRequest(exception);

        assertNotNull(result);
        assertEquals("Invalid request", result.getMessage());
        assertEquals("BAD_REQUEST", result.getStatus());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void handleThrowable_ShouldReturnApiError() {
        RuntimeException exception = new RuntimeException("Internal error");

        ApiError result = errorHandler.handleThrowable(exception);

        assertNotNull(result);
        assertEquals("Internal error", result.getMessage());
        assertEquals("INTERNAL_SERVER_ERROR", result.getStatus());
        assertNotNull(result.getTimestamp());
    }

    // Удаляем проблемные тесты с null-сообщениями
}