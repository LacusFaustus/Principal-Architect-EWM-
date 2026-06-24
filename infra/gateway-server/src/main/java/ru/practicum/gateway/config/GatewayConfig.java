package ru.practicum.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import ru.practicum.gateway.security.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Используем Principal из SecurityContext
            return exchange.getPrincipal()
                    .map(principal -> "user:" + principal.getName())
                    .switchIfEmpty(Mono.just("anonymous"));
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service - публичные эндпоинты
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("authServiceCB")
                                        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("lb://auth-service"))

                // User Service - проверка JWT
                .route("user-service", r -> r
                        .path("/admin/users/**", "/users/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .circuitBreaker(config -> config
                                        .setName("userServiceCB")
                                        .setFallbackUri("forward:/fallback/users"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://user-service"))

                // Event Service - проверка JWT
                .route("event-service", r -> r
                        .path("/admin/categories/**", "/categories/**",
                                "/admin/compilations/**", "/compilations/**",
                                "/admin/events/**", "/events/**", "/users/*/events/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .circuitBreaker(config -> config
                                        .setName("eventServiceCB")
                                        .setFallbackUri("forward:/fallback/events"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://event-service"))

                // Request Service - проверка JWT
                .route("request-service", r -> r
                        .path("/users/*/requests/**", "/users/*/events/*/requests/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .circuitBreaker(config -> config
                                        .setName("requestServiceCB")
                                        .setFallbackUri("forward:/fallback/requests"))
                        )
                        .uri("lb://request-service"))

                // Comment Service - проверка JWT
                .route("comment-service", r -> r
                        .path("/comments/**", "/admin/comments/**", "/users/*/comments/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .circuitBreaker(config -> config
                                        .setName("commentServiceCB")
                                        .setFallbackUri("forward:/fallback/comments"))
                        )
                        .uri("lb://comment-service"))

                // Stats Service - публичные эндпоинты
                .route("stats-server", r -> r
                        .path("/hit", "/stats")
                        .uri("lb://stats-server"))
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}