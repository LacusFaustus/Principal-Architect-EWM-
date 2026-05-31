package ru.practicum.gateway.fallback;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/users")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return Mono.just(ResponseEntity.ok()
                .body(Collections.singletonMap("message",
                        "User service is temporarily unavailable. Please try again later.")));
    }

    @RequestMapping("/fallback/events")
    public Mono<ResponseEntity<Map<String, Object>>> eventServiceFallback() {
        return Mono.just(ResponseEntity.ok()
                .body(Collections.singletonMap("message",
                        "Event service is temporarily unavailable. Please try again later.")));
    }

    @RequestMapping("/fallback/requests")
    public Mono<ResponseEntity<Map<String, Object>>> requestServiceFallback() {
        return Mono.just(ResponseEntity.ok()
                .body(Collections.singletonMap("message",
                        "Request service is temporarily unavailable. Please try again later.")));
    }

    @RequestMapping("/fallback/comments")
    public Mono<ResponseEntity<Map<String, Object>>> commentServiceFallback() {
        return Mono.just(ResponseEntity.ok()
                .body(Collections.singletonMap("message",
                        "Comment service is temporarily unavailable. Please try again later.")));
    }
}