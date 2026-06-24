package ru.practicum.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.event.events.EventCreatedEvent;
import ru.practicum.event.events.EventPublishedEvent;
import ru.practicum.event.events.EventUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String EVENT_CREATED_TOPIC = "events.created.v1";
    private static final String EVENT_PUBLISHED_TOPIC = "events.published.v1";
    private static final String EVENT_UPDATED_TOPIC = "events.updated.v1";

    public void publishEventCreated(EventCreatedEvent event) {
        try {
            kafkaTemplate.send(EVENT_CREATED_TOPIC, String.valueOf(event.getEventId()), event);
            log.info("Published EventCreated event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish EventCreated event: {}", e.getMessage(), e);
        }
    }

    public void publishEventPublished(EventPublishedEvent event) {
        try {
            kafkaTemplate.send(EVENT_PUBLISHED_TOPIC, String.valueOf(event.getEventId()), event);
            log.info("Published EventPublished event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish EventPublished event: {}", e.getMessage(), e);
        }
    }

    public void publishEventUpdated(EventUpdatedEvent event) {
        try {
            kafkaTemplate.send(EVENT_UPDATED_TOPIC, String.valueOf(event.getEventId()), event);
            log.info("Published EventUpdated event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish EventUpdated event: {}", e.getMessage(), e);
        }
    }
}