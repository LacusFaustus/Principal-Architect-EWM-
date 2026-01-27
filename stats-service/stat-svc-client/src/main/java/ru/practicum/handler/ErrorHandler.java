package ru.practicum.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleStatClientException(RuntimeException ex) {
        log.error("Внутренняя ошибка сервера: ", ex);
        return new ApiError(LocalDateTime.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Stats Service Unavailable", ex.getMessage());

    }
}
