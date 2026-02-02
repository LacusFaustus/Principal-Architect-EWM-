package ru.practicum.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.handler.exception.BadRequestException;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 404: Объект не найден
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        log.error("404 Not Found: {}", e.getMessage());
        return new ApiError(
                getStackTrace(e),
                e.getMessage(),
                "The required object was not found.",
                HttpStatus.NOT_FOUND.name(),
                LocalDateTime.now().format(FORMATTER)
        );
    }

    // 409: Конфликт (бизнес-логика или уникальность в БД)
    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final Exception e) {
        log.error("409 Conflict: {}", e.getMessage());
        return new ApiError(
                getStackTrace(e),
                e.getMessage(),
                "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT.name(),
                LocalDateTime.now().format(FORMATTER)
        );
    }

    // 400: Нарушение валидации или условий запроса
    @ExceptionHandler({BadRequestException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(final Exception e) {
        log.error("400 Bad Request: {}", e.getMessage());
        return new ApiError(
                getStackTrace(e),
                e.getMessage(),
                "Incorrectly made request.",
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now().format(FORMATTER)
        );
    }

    // 500: Все остальные непредвиденные ошибки
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable e) {
        log.error("500 Internal Server Error: {}", e.getMessage(), e);
        return new ApiError(
                getStackTrace(e),
                e.getMessage(),
                "Error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                LocalDateTime.now().format(FORMATTER)
        );
    }

    private List<String> getStackTrace(Throwable e) {
        return Arrays.stream(e.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
    }
}
