package ru.practicum.saga.exception;

public class SagaRollbackException extends RuntimeException {

    public SagaRollbackException(String message) {
        super(message);
    }

    public SagaRollbackException(String message, Throwable cause) {
        super(message, cause);
    }
}