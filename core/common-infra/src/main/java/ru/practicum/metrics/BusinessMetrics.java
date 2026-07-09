package ru.practicum.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
public class BusinessMetrics {

    private final Counter eventCreatedCounter;
    private final Counter eventPublishedCounter;
    private final Counter eventViewedCounter;
    private final Counter eventLikedCounter;
    private final Counter userRegisteredCounter;
    private final Counter commentCreatedCounter;
    private final Counter requestCreatedCounter;
    private final Counter requestConfirmedCounter;
    private final Counter requestRejectedCounter;

    private final Timer eventProcessingTimer;
    private final Timer dbQueryTimer;
    private final Timer externalApiTimer;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        // Events
        this.eventCreatedCounter = Counter.builder("events.created.total")
                .description("Total number of events created")
                .tag("type", "event")
                .register(meterRegistry);

        this.eventPublishedCounter = Counter.builder("events.published.total")
                .description("Total number of events published")
                .tag("type", "event")
                .register(meterRegistry);

        this.eventViewedCounter = Counter.builder("events.viewed.total")
                .description("Total number of event views")
                .tag("type", "event")
                .register(meterRegistry);

        this.eventLikedCounter = Counter.builder("events.liked.total")
                .description("Total number of event likes")
                .tag("type", "event")
                .register(meterRegistry);

        // Users
        this.userRegisteredCounter = Counter.builder("users.registered.total")
                .description("Total number of registered users")
                .tag("type", "user")
                .register(meterRegistry);

        // Comments
        this.commentCreatedCounter = Counter.builder("comments.created.total")
                .description("Total number of comments created")
                .tag("type", "comment")
                .register(meterRegistry);

        // Requests
        this.requestCreatedCounter = Counter.builder("requests.created.total")
                .description("Total number of participation requests created")
                .tag("type", "request")
                .register(meterRegistry);

        this.requestConfirmedCounter = Counter.builder("requests.confirmed.total")
                .description("Total number of confirmed participation requests")
                .tag("type", "request")
                .register(meterRegistry);

        this.requestRejectedCounter = Counter.builder("requests.rejected.total")
                .description("Total number of rejected participation requests")
                .tag("type", "request")
                .register(meterRegistry);

        // Timers
        this.eventProcessingTimer = Timer.builder("events.processing.time")
                .description("Time taken to process event operations")
                .register(meterRegistry);

        this.dbQueryTimer = Timer.builder("db.query.time")
                .description("Time taken for database queries")
                .register(meterRegistry);

        this.externalApiTimer = Timer.builder("external.api.time")
                .description("Time taken for external API calls")
                .register(meterRegistry);
    }

    // ============================================================
    // EVENT METRICS
    // ============================================================

    public void recordEventCreated() {
        eventCreatedCounter.increment();
    }

    public void recordEventPublished() {
        eventPublishedCounter.increment();
    }

    public void recordEventViewed() {
        eventViewedCounter.increment();
    }

    public void recordEventLiked() {
        eventLikedCounter.increment();
    }

    // ============================================================
    // USER METRICS
    // ============================================================

    public void recordUserRegistered() {
        userRegisteredCounter.increment();
    }

    // ============================================================
    // COMMENT METRICS
    // ============================================================

    public void recordCommentCreated() {
        commentCreatedCounter.increment();
    }

    // ============================================================
    // REQUEST METRICS
    // ============================================================

    public void recordRequestCreated() {
        requestCreatedCounter.increment();
    }

    public void recordRequestConfirmed() {
        requestConfirmedCounter.increment();
    }

    public void recordRequestRejected() {
        requestRejectedCounter.increment();
    }

    // ============================================================
    // TIMER METHODS
    // ============================================================

    public <T> T recordEventProcessing(Callable<T> callable) throws Exception {
        return eventProcessingTimer.recordCallable(callable);
    }

    public void recordEventProcessing(Runnable runnable) {
        eventProcessingTimer.record(runnable);
    }

    public <T> T recordDbQuery(Callable<T> callable) throws Exception {
        return dbQueryTimer.recordCallable(callable);
    }

    public <T> T recordExternalApi(Callable<T> callable) throws Exception {
        return externalApiTimer.recordCallable(callable);
    }

    // ============================================================
    // TIMER WITH TIME UNIT
    // ============================================================

    public <T> T recordEventProcessing(Callable<T> callable, TimeUnit timeUnit) throws Exception {
        return eventProcessingTimer.recordCallable(callable);
    }
}