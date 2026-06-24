package ru.practicum.eventcontext.domain;

import lombok.Getter;
import lombok.Setter;
import ru.practicum.eventcontext.events.EventCanceledEvent;
import ru.practicum.eventcontext.events.EventCreatedEvent;
import ru.practicum.eventcontext.events.EventPublishedEvent;

import java.time.LocalDateTime;

/**
 * Агрегат Event для DDD подхода.
 * Содержит бизнес-логику управления событием.
 */
@Getter
@Setter
public class Event {

    private String id;
    private String title;
    private String annotation;
    private String description;
    private String category;
    private Long initiatorId;
    private EventStatus status;
    private Integer participantLimit;
    private Boolean requestModeration;
    private Boolean paid;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime eventDate;
    private Double lat;
    private Double lon;

    /**
     * Создание нового события
     */
    public void create(CreateEventCommand command) {
        validateCreateCommand(command);

        apply(new EventCreatedEvent(
                command.getId(),
                command.getTitle(),
                command.getAnnotation(),
                command.getDescription(),
                command.getCategory(),
                command.getInitiatorId(),
                command.getParticipantLimit(),
                command.getRequestModeration(),
                command.getPaid(),
                command.getEventDate(),
                LocalDateTime.now(),
                command.getLat(),
                command.getLon()
        ));
    }

    /**
     * Публикация события (администратор)
     */
    public void publish() {
        if (status != EventStatus.PENDING) {
            throw DomainException.withMessage(
                    "Cannot publish event in status: %s. Only PENDING events can be published.",
                    status
            );
        }

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw DomainException.withMessage(
                    "Cannot publish event with date in less than 2 hours: %s",
                    eventDate
            );
        }

        apply(new EventPublishedEvent(id, LocalDateTime.now()));
    }

    /**
     * Отмена события (администратор или пользователь)
     */
    public void cancel(String reason) {
        if (status == EventStatus.PUBLISHED) {
            throw DomainException.withMessage(
                    "Cannot cancel published event. Event ID: %s",
                    id
            );
        }

        apply(new EventCanceledEvent(id, reason, LocalDateTime.now()));
    }

    /**
     * Обновление события (пользователь)
     */
    public void update(UpdateEventCommand command) {
        if (status == EventStatus.PUBLISHED) {
            throw DomainException.withMessage(
                    "Cannot update published event. Event ID: %s",
                    id
            );
        }

        if (command.getTitle() != null && !command.getTitle().isBlank()) {
            this.title = command.getTitle();
        }

        if (command.getAnnotation() != null && !command.getAnnotation().isBlank()) {
            this.annotation = command.getAnnotation();
        }

        if (command.getDescription() != null && !command.getDescription().isBlank()) {
            this.description = command.getDescription();
        }

        if (command.getEventDate() != null) {
            if (command.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw DomainException.withMessage(
                        "Event date must be at least 2 hours in the future"
                );
            }
            this.eventDate = command.getEventDate();
        }

        if (command.getCategory() != null && !command.getCategory().isBlank()) {
            this.category = command.getCategory();
        }
    }

    /**
     * Проверка, может ли пользователь участвовать
     */
    public boolean canParticipate(Long userId) {
        if (status != EventStatus.PUBLISHED) {
            return false;
        }

        if (initiatorId.equals(userId)) {
            return false;
        }

        if (eventDate.isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * Проверка, есть ли еще места
     */
    public boolean hasAvailableSlots(long confirmedRequests) {
        if (participantLimit == 0) {
            return true;
        }
        return confirmedRequests < participantLimit;
    }

    /**
     * Проверка, требуется ли модерация заявок
     */
    public boolean requiresModeration() {
        return requestModeration != null && requestModeration;
    }

    /**
     * Применение событий (Event Sourcing)
     */
    private void apply(EventCreatedEvent event) {
        this.id = event.getEventId();
        this.title = event.getTitle();
        this.annotation = event.getAnnotation();
        this.description = event.getDescription();
        this.category = event.getCategory();
        this.initiatorId = event.getInitiatorId();
        this.participantLimit = event.getParticipantLimit();
        this.requestModeration = event.getRequestModeration();
        this.paid = event.getPaid();
        this.eventDate = event.getEventDate();
        this.lat = event.getLat();
        this.lon = event.getLon();
        this.status = EventStatus.PENDING;
        this.createdAt = event.getCreatedAt();
    }

    private void apply(EventPublishedEvent event) {
        this.status = EventStatus.PUBLISHED;
        this.publishedAt = event.getPublishedAt();
    }

    private void apply(EventCanceledEvent event) {
        this.status = EventStatus.CANCELED;
    }

    /**
     * Валидация команды создания
     */
    private void validateCreateCommand(CreateEventCommand command) {
        if (command.getTitle() == null || command.getTitle().isBlank()) {
            throw DomainException.withMessage("Event title cannot be empty");
        }

        if (command.getTitle().length() < 3 || command.getTitle().length() > 120) {
            throw DomainException.withMessage("Event title must be between 3 and 120 characters");
        }

        if (command.getAnnotation() == null || command.getAnnotation().isBlank()) {
            throw DomainException.withMessage("Event annotation cannot be empty");
        }

        if (command.getAnnotation().length() < 20 || command.getAnnotation().length() > 2000) {
            throw DomainException.withMessage("Event annotation must be between 20 and 2000 characters");
        }

        if (command.getEventDate() == null) {
            throw DomainException.withMessage("Event date cannot be null");
        }

        if (command.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw DomainException.withMessage("Event date must be at least 2 hours in the future");
        }

        if (command.getParticipantLimit() == null || command.getParticipantLimit() < 0) {
            throw DomainException.withMessage("Participant limit cannot be negative");
        }
    }

    // Вспомогательные классы для команд

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CreateEventCommand {
        private String id;
        private String title;
        private String annotation;
        private String description;
        private String category;
        private Long initiatorId;
        private Integer participantLimit;
        private Boolean requestModeration;
        private Boolean paid;
        private LocalDateTime eventDate;
        private Double lat;
        private Double lon;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UpdateEventCommand {
        private String title;
        private String annotation;
        private String description;
        private String category;
        private LocalDateTime eventDate;
        private Double lat;
        private Double lon;
    }
}