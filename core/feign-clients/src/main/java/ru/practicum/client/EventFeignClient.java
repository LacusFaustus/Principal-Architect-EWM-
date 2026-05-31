package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.EventInfoDto;

@FeignClient(name = "event-service", fallback = EventFeignClientFallback.class)
public interface EventFeignClient {

    @GetMapping("/internal/events/{eventId}")
    EventInfoDto getEventById(@PathVariable("eventId") Long eventId);
}