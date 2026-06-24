package ru.practicum.analyzer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.events.EventCreatedEvent;
import ru.practicum.events.EventPublishedEvent;
import ru.practicum.events.EventUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventEventListener {

    private final RecommendationService recommendationService;

    @KafkaListener(
            topics = "events.created.v1",
            groupId = "recommendation-analyzer"
    )
    public void handleEventCreated(EventCreatedEvent event) {
        log.info("Received EventCreated event: {}", event);
        recommendationService.initializeEvent(event.getEventId());
    }

    @KafkaListener(
            topics = "events.published.v1",
            groupId = "recommendation-analyzer"
    )
    public void handleEventPublished(EventPublishedEvent event) {
        log.info("Received EventPublished event: {}", event);
        recommendationService.activateEvent(event.getEventId());
    }

    @KafkaListener(
            topics = "events.updated.v1",
            groupId = "recommendation-analyzer"
    )
    public void handleEventUpdated(EventUpdatedEvent event) {
        log.info("Received EventUpdated event: {}", event);
        recommendationService.updateEventMetadata(event.getEventId());
    }
}