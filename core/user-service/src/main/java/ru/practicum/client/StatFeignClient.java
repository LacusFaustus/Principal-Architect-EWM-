package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.NewEndpointHitDto;

@FeignClient(name = "stats-server", url = "${stats-server.url:http://localhost:9090}")
public interface StatFeignClient {

    @PostMapping("/hit")
    void saveHit(@RequestBody NewEndpointHitDto hitDto);
}