package ru.practicum.eventsourcing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStore {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void append(String aggregateId, Object event) {
        try {
            String topic = event.getClass().getSimpleName().toLowerCase().replace("event", "") + ".v1";
            String key = aggregateId;

            kafkaTemplate.send(topic, key, event);
            log.info("Event appended: topic={}, key={}, event={}", topic, key, event.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to append event: {}", e.getMessage(), e);
            throw new EventStoreException("Failed to append event", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T aggregate(Class<T> aggregateClass, String aggregateId, List<Object> events) {
        try {
            T aggregate = aggregateClass.getDeclaredConstructor().newInstance();
            for (Object event : events) {
                Method applyMethod = findApplyMethod(aggregateClass, event.getClass());
                if (applyMethod != null) {
                    applyMethod.invoke(aggregate, event);
                } else {
                    log.warn("No apply method found for event: {}", event.getClass().getSimpleName());
                }
            }
            return aggregate;
        } catch (Exception e) {
            throw new EventStoreException("Failed to rebuild aggregate", e);
        }
    }

    private Method findApplyMethod(Class<?> aggregateClass, Class<?> eventClass) {
        try {
            return aggregateClass.getMethod("apply", eventClass);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}