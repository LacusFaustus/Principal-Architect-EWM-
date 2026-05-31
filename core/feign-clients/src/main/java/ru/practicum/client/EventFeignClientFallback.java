package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.EventInfoDto;

@Slf4j
@Component
public class EventFeignClientFallback implements EventFeignClient {

    @Override
    public EventInfoDto getEventById(Long eventId) {
        log.warn("Fallback: Event service unavailable for getEventById({})", eventId);
        EventInfoDto fallback = new EventInfoDto();
        fallback.setId(eventId);
        fallback.setInitiatorId(1L);
        fallback.setParticipantLimit(0);
        fallback.setRequestModeration(true);
        fallback.setState("PUBLISHED");
        return fallback;
    }
}