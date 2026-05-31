package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatFeignClientFallback implements StatFeignClient {

    @Override
    public void saveHit(NewEndpointHitDto hitDto) {
        log.warn("Fallback: Stats service unavailable for saveHit({})", hitDto);
        // Do nothing - stats are optional
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.warn("Fallback: Stats service unavailable for getStats({}, {}, {}, {})", start, end, uris, unique);
        return Collections.emptyList();
    }
}