package ru.practicum.eventcontext.domain;

/**
 * Статусы события в доменной модели
 */
public enum EventStatus {

    /**
     * Ожидает модерации
     */
    PENDING("Ожидает модерации"),

    /**
     * Опубликовано
     */
    PUBLISHED("Опубликовано"),

    /**
     * Отменено
     */
    CANCELED("Отменено"),

    /**
     * Отклонено модератором
     */
    REJECTED("Отклонено");

    private final String description;

    EventStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Проверка, может ли событие быть опубликовано
     */
    public boolean canBePublished() {
        return this == PENDING;
    }

    /**
     * Проверка, доступно ли событие для просмотра
     */
    public boolean isAvailable() {
        return this == PUBLISHED;
    }

    /**
     * Проверка, может ли событие быть изменено
     */
    public boolean canBeModified() {
        return this == PENDING || this == REJECTED;
    }
}