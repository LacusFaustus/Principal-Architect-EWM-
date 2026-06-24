package ru.practicum.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class UserKeyResolver {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Используем userId из заголовка, если есть
            String userId = exchange.getRequest().getHeaders().getFirst("X-USER-ID");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            // Иначе используем IP адрес
            String ip = Objects.requireNonNull(
                    exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }
}