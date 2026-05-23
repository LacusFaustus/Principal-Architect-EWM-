package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.List;

@FeignClient(name = "stats-server") public interface StatFeignClient {

    @PostMapping("/hit")
    void saveHit(@RequestBody NewEndpointHitDto hitDto);

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(@RequestParam("start") String start,
                                @RequestParam("end") String end,
                                @RequestParam(value = "uris", required = false) List<String> uris,
                                @RequestParam(value = "unique", defaultValue = "false") boolean unique);
}