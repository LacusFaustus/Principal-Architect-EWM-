package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.StatFeignClient;
import ru.practicum.dto.NewEndpointHitDto;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final StatFeignClient statFeignClient;

    @GetMapping("/feign-test")
    public ResponseEntity<String> testFeign() {
        try {
            NewEndpointHitDto hit = NewEndpointHitDto.builder()
                    .app("user-service")
                    .uri("/test/feign-test")
                    .ip("127.0.0.1")
                    .timestamp(LocalDateTime.now())
                    .build();

            statFeignClient.saveHit(hit);
            log.info("Feign client работает успешно!");
            return ResponseEntity.ok("Feign client is working!");
        } catch (Exception e) {
            log.error("Feign client error: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Feign client error: " + e.getMessage());
        }
    }
}