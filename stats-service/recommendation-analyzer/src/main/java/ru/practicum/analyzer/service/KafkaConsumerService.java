package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    // Веса действий
    private static final double WEIGHT_VIEW = 0.4;
    private static final double WEIGHT_REGISTER = 0.8;
    private static final double WEIGHT_LIKE = 1.0;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "analyzer-group")
    @Transactional
    public void processUserAction(UserActionAvro action) {
        try {
            Long userId = action.getUserId();
            Long eventId = action.getEventId();
            double weight = getWeight(action.getActionType());

            log.info("Processing user action: userId={}, eventId={}, weight={}", userId, eventId, weight);

            UserAction existingAction = userActionRepository
                    .findByUserIdAndEventId(userId, eventId)
                    .orElse(null);

            if (existingAction == null) {
                // Создаем новое действие
                UserAction newAction = UserAction.builder()
                        .userId(userId)
                        .eventId(eventId)
                        .weight(weight)
                        .actionType(action.getActionType().name())
                        .lastActionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(action.getTimestamp()), ZoneId.systemDefault()))
                        .build();
                userActionRepository.save(newAction);
                log.info("Saved new user action: {}", newAction);
            } else if (weight > existingAction.getWeight()) {
                // Обновляем вес
                existingAction.setWeight(weight);
                existingAction.setActionType(action.getActionType().name());
                existingAction.setLastActionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(action.getTimestamp()), ZoneId.systemDefault()));
                userActionRepository.save(existingAction);
                log.info("Updated user action: {}", existingAction);
            } else {
                log.debug("Weight not increased for userId={}, eventId={}, oldWeight={}, newWeight={}",
                        userId, eventId, existingAction.getWeight(), weight);
            }
        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.events-similarity}", groupId = "analyzer-group")
    @Transactional
    public void processEventSimilarity(EventSimilarityAvro similarity) {
        try {
            Long eventA = similarity.getEventA();
            Long eventB = similarity.getEventB();
            Double score = similarity.getScore();

            log.info("Processing event similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);

            EventSimilarity existingSimilarity = eventSimilarityRepository
                    .findByEventAAndEventB(eventA, eventB)
                    .orElse(null);

            if (existingSimilarity == null) {
                EventSimilarity newSimilarity = EventSimilarity.builder()
                        .eventA(eventA)
                        .eventB(eventB)
                        .score(score)
                        .updated(LocalDateTime.ofInstant(Instant.ofEpochMilli(similarity.getTimestamp()), ZoneId.systemDefault()))
                        .build();
                eventSimilarityRepository.save(newSimilarity);
                log.info("Saved new event similarity: {}", newSimilarity);
            } else {
                existingSimilarity.setScore(score);
                existingSimilarity.setUpdated(LocalDateTime.ofInstant(Instant.ofEpochMilli(similarity.getTimestamp()), ZoneId.systemDefault()));
                eventSimilarityRepository.save(existingSimilarity);
                log.info("Updated event similarity: {}", existingSimilarity);
            }
        } catch (Exception e) {
            log.error("Error processing event similarity", e);
        }
    }

    private double getWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return WEIGHT_VIEW;
            case REGISTER:
                return WEIGHT_REGISTER;
            case LIKE:
                return WEIGHT_LIKE;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }
}