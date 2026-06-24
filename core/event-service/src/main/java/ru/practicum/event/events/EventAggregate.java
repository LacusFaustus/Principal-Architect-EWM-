package ru.practicum.event.events;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.eventsourcing.EventStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class EventAggregate {

    private String id;
    private String title;
    private String annotation;
    private String description;
    private String category;
    private Long initiatorId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private Integer participantLimit;
    private Boolean requestModeration;
    private Boolean paid;
    private List<Object> events = new ArrayList<>();
    private EventStore eventStore;

    public void createEvent(CreateEventCommand command) {
        if (id != null) {
            throw new IllegalStateException("Event already exists");
        }

        EventCreatedEvent event = new EventCreatedEvent(
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
                LocalDateTime.now()
        );

        apply(event);
        eventStore.append(command.getId(), event);
        log.info("Event created: {}", id);
    }

    public void publishEvent(PublishEventCommand command) {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Event must be in PENDING state to publish");
        }

        EventPublishedEvent event = new EventPublishedEvent(
                id,
                LocalDateTime.now()
        );

        apply(event);
        eventStore.append(id, event);
        log.info("Event published: {}", id);
    }

    public void cancelEvent(CancelEventCommand command) {
        if ("PUBLISHED".equals(status)) {
            throw new IllegalStateException("Cannot cancel published event");
        }

        EventCanceledEvent event = new EventCanceledEvent(
                id,
                command.getReason(),
                LocalDateTime.now()
        );

        apply(event);
        eventStore.append(id, event);
        log.info("Event canceled: {}", id);
    }

    public void updateEvent(UpdateEventCommand command) {
        if ("PUBLISHED".equals(status)) {
            throw new IllegalStateException("Cannot update published event");
        }

        EventUpdatedEvent event = new EventUpdatedEvent(
                id,
                command.getNewTitle(),
                command.getNewAnnotation(),
                command.getNewDescription(),
                LocalDateTime.now()
        );

        apply(event);
        eventStore.append(id, event);
        log.info("Event updated: {}", id);
    }

    public void apply(EventCreatedEvent event) {
        this.id = event.getEventId();
        this.title = event.getTitle();
        this.annotation = event.getAnnotation();
        this.description = event.getDescription();
        this.category = event.getCategory();
        this.initiatorId = event.getInitiatorId();
        this.participantLimit = event.getParticipantLimit();
        this.requestModeration = event.getRequestModeration();
        this.paid = event.getPaid();
        this.status = "PENDING";
        this.createdAt = event.getCreatedAt();
        events.add(event);
    }

    public void apply(EventPublishedEvent event) {
        this.status = "PUBLISHED";
        this.publishedAt = event.getPublishedAt();
        this.updatedAt = event.getPublishedAt();
        events.add(event);
    }

    public void apply(EventCanceledEvent event) {
        this.status = "CANCELED";
        this.updatedAt = event.getCanceledAt();
        events.add(event);
    }

    public void apply(EventUpdatedEvent event) {
        if (event.getNewTitle() != null) {
            this.title = event.getNewTitle();
        }
        if (event.getNewAnnotation() != null) {
            this.annotation = event.getNewAnnotation();
        }
        if (event.getNewDescription() != null) {
            this.description = event.getNewDescription();
        }
        this.updatedAt = event.getUpdatedAt();
        events.add(event);
    }
}