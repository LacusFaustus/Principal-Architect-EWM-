package ru.practicum.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        log.error("Server error: ", exception);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();

        return new ApiError(List.of(stackTrace), exception.getMessage(), "Server error", HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                LocalDateTime.now().format(FORMATTER));

    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final RuntimeException exception) {
        log.error("NotFoundError: {}", exception.getMessage());

        ApiError error = handleException(exception);
        error.setReason("The required object was not found.");
        error.setStatus(HttpStatus.NOT_FOUND.toString());

        return error;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationExceptions(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> "Field: " + error.getField() + " Error: " + error.getDefaultMessage()
                        + " Value: " + error.getRejectedValue())
                .toList();

        String errorMessage = String.join("; ", errors);

        log.error("ValidationError: {}", errorMessage);

        ApiError error = handleException(exception);
        error.setMessage(errorMessage);
        error.setReason("Incorrectly made request.");
        error.setStatus(HttpStatus.BAD_REQUEST.toString());

        return error;
    }

    /*@ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(DataIntegrityViolationException exception, HttpStatus status) {
        log.error("BadRequestException: ", exception);

        String constraintName = extractConstraintName(exception);

        ApiError error = handleException(exception, status);
        error.setReason("Integrity constraint has been violated.");
        error.setMessage("Could not execute statement; SQL [n/a]; constraint " + constraintName +
                "; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement");

        return error;
    }

    private String extractConstraintName(DataIntegrityViolationException exception) {
        if (exception.getCause() instanceof ConstraintViolationException) {
            ConstraintViolationException cve =
                    (ConstraintViolationException) exception.getCause();
            return cve.getConstraintName();
        }

        return null;
    } */
}
