package ru.practicum.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BusinessMetrics {

    private final Counter eventCreatedCounter;
    private final Counter eventPublishedCounter;
    private final Counter eventViewedCounter;
    private final Counter userRegisteredCounter;
    private final Counter commentCreatedCounter;
    private final Timer eventProcessingTimer;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.eventCreatedCounter = Counter.builder("events.created.total")
                .description("Total number of events created")
                .register(meterRegistry);

        this.eventPublishedCounter = Counter.builder("events.published.total")
                .description("Total number of events published")
                .register(meterRegistry);

        this.eventViewedCounter = Counter.builder("events.viewed.total")
                .description("Total number of event views")
                .register(meterRegistry);

        this.userRegisteredCounter = Counter.builder("users.registered.total")
                .description("Total number of registered users")
                .register(meterRegistry);

        this.commentCreatedCounter = Counter.builder("comments.created.total")
                .description("Total number of comments created")
                .register(meterRegistry);

        this.eventProcessingTimer = Timer.builder("events.processing.time")
                .description("Time taken to process event operations")
                .register(meterRegistry);
    }

    public void recordEventCreated() {
        eventCreatedCounter.increment();
    }

    public void recordEventPublished() {
        eventPublishedCounter.increment();
    }

    public void recordEventViewed() {
        eventViewedCounter.increment();
    }

    public void recordUserRegistered() {
        userRegisteredCounter.increment();
    }

    public void recordCommentCreated() {
        commentCreatedCounter.increment();
    }

    public <T> T recordEventProcessing(java.util.concurrent.Callable<T> callable) throws Exception {
        return eventProcessingTimer.recordCallable(callable);
    }

    public void recordEventProcessing(Runnable runnable) {
        eventProcessingTimer.record(() -> {
            runnable.run();
        });
    }
}