package ru.practicum.eventcontext.domain;

/**
 * Базовое исключение для доменной логики.
 * Используется для ошибок, связанных с бизнес-правилами и инвариантами.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Создает исключение с форматированным сообщением
     */
    public static DomainException withMessage(String format, Object... args) {
        return new DomainException(String.format(format, args));
    }
}