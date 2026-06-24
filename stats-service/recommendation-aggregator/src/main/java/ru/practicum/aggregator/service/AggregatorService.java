package ru.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AggregatorService {

    // Веса действий
    private static final double WEIGHT_VIEW = 0.4;
    private static final double WEIGHT_REGISTER = 0.8;
    private static final double WEIGHT_LIKE = 1.0;

    // user -> event -> maxWeight
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();

    // event -> totalWeight (S_a)
    private final Map<Long, Double> eventTotalWeights = new ConcurrentHashMap<>();

    // eventA (min) -> eventB (max) -> S_min
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.events-similarity:stats.events-similarity.v1}")
    private String eventsSimilarityTopic;

    @Autowired
    public AggregatorService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topics.user-actions:stats.user-actions.v1}", groupId = "aggregator-group")
    public void processUserAction(Map<String, Object> action) {
        Long userId = ((Number) action.get("userId")).longValue();
        Long eventId = ((Number) action.get("eventId")).longValue();
        String actionTypeStr = (String) action.get("actionType");
        double newWeight = getWeight(actionTypeStr);

        log.info("Processing user action: userId={}, eventId={}, actionType={}, newWeight={}",
                userId, eventId, actionTypeStr, newWeight);

        // Получаем старый вес пользователя для этого события
        Map<Long, Double> userEvents = userEventWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        Double oldWeight = userEvents.get(eventId);

        // Если вес не изменился или новый не больше старого - ничего не делаем
        if (oldWeight != null && newWeight <= oldWeight) {
            log.debug("Weight not changed for userId={}, eventId={}, oldWeight={}, newWeight={}",
                    userId, eventId, oldWeight, newWeight);
            return;
        }

        // Обновляем вес пользователя
        userEvents.put(eventId, newWeight);
        log.debug("Updated user weight: userId={}, eventId={}, weight={}", userId, eventId, newWeight);

        // Обновляем общую сумму весов для события
        double deltaWeight = (oldWeight == null ? newWeight : newWeight - oldWeight);
        eventTotalWeights.merge(eventId, deltaWeight, Double::sum);
        log.debug("Updated total weight for eventId={}: delta={}, newTotal={}",
                eventId, deltaWeight, eventTotalWeights.get(eventId));

        // Если у пользователя было старое значение, нужно пересчитать связи с другими событиями
        if (oldWeight != null) {
            recalculateSimilaritiesWithUpdate(eventId, userId, oldWeight, newWeight);
        } else {
            recalculateSimilaritiesWithNew(eventId, userId, newWeight);
        }
    }

    private void recalculateSimilaritiesWithNew(Long updatedEvent, Long userId, Double newWeight) {
        Map<Long, Double> userEvents = userEventWeights.get(userId);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
            Long otherEvent = entry.getKey();
            if (otherEvent.equals(updatedEvent)) continue;

            double otherWeight = entry.getValue();

            long eventA = Math.min(updatedEvent, otherEvent);
            long eventB = Math.max(updatedEvent, otherEvent);

            double oldMin = minWeightsSums
                    .computeIfAbsent(eventA, k -> new ConcurrentHashMap<>())
                    .getOrDefault(eventB, 0.0);

            double newMin = Math.min(newWeight, otherWeight);
            double deltaMin = newMin - oldMin;

            if (deltaMin != 0) {
                minWeightsSums.get(eventA).merge(eventB, deltaMin, Double::sum);
                log.debug("Updated S_min for pair ({},{}): oldMin={}, newMin={}, delta={}",
                        eventA, eventB, oldMin, newMin, deltaMin);

                double similarity = calculateSimilarity(eventA, eventB);
                sendSimilarityUpdate(eventA, eventB, similarity);
            }
        }
    }

    private void recalculateSimilaritiesWithUpdate(Long updatedEvent, Long userId, Double oldWeight, Double newWeight) {
        Map<Long, Double> userEvents = userEventWeights.get(userId);

        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
            Long otherEvent = entry.getKey();
            if (otherEvent.equals(updatedEvent)) continue;

            double otherWeight = entry.getValue();

            long eventA = Math.min(updatedEvent, otherEvent);
            long eventB = Math.max(updatedEvent, otherEvent);

            double oldPairMin = Math.min(oldWeight, otherWeight);
            double newPairMin = Math.min(newWeight, otherWeight);
            double deltaMin = newPairMin - oldPairMin;

            if (deltaMin != 0) {
                minWeightsSums
                        .computeIfAbsent(eventA, k -> new ConcurrentHashMap<>())
                        .merge(eventB, deltaMin, Double::sum);
                log.debug("Updated S_min for pair ({},{}): delta={}", eventA, eventB, deltaMin);

                double similarity = calculateSimilarity(eventA, eventB);
                sendSimilarityUpdate(eventA, eventB, similarity);
            }
        }
    }

    private double calculateSimilarity(Long eventA, Long eventB) {
        Double totalWeightA = eventTotalWeights.get(eventA);
        Double totalWeightB = eventTotalWeights.get(eventB);
        Double sMin = minWeightsSums.getOrDefault(eventA, Map.of()).get(eventB);

        if (totalWeightA == null || totalWeightB == null || sMin == null || totalWeightA == 0 || totalWeightB == 0) {
            return 0.0;
        }

        double similarity = sMin / (Math.sqrt(totalWeightA) * Math.sqrt(totalWeightB));
        log.debug("Calculated similarity for pair ({},{}): sMin={}, sA={}, sB={}, similarity={}",
                eventA, eventB, sMin, totalWeightA, totalWeightB, similarity);

        return similarity;
    }

    private void sendSimilarityUpdate(Long eventA, Long eventB, double similarity) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventA", eventA);
        message.put("eventB", eventB);
        message.put("score", similarity);
        message.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(eventsSimilarityTopic, String.valueOf(eventA), message);
        log.debug("Sent similarity update to Kafka: eventA={}, eventB={}, similarity={}", eventA, eventB, similarity);
    }

    private double getWeight(String actionType) {
        if (actionType == null) return 0.0;
        switch (actionType) {
            case "ACTION_VIEW":
                return WEIGHT_VIEW;
            case "ACTION_REGISTER":
                return WEIGHT_REGISTER;
            case "ACTION_LIKE":
                return WEIGHT_LIKE;
            default:
                return 0.0;
        }
    }
}