package ru.practicum.request.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventServiceClientFallback implements EventServiceClient {

    @Override
    public Object getEventById(Long eventId) {
        log.warn("Fallback: Event service unavailable for getEventById({})", eventId);
        return null;
    }
}