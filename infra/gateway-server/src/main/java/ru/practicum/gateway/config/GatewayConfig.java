package ru.practicum.gateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import ru.practicum.gateway.filter.RoleValidationFilter;
import ru.practicum.gateway.security.JwtAuthenticationFilter;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RoleValidationFilter roleValidationFilter;

    // ============================================================
    // RATE LIMITING
    // ============================================================

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-USER-ID");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just("ip:" + remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("ip:anonymous");
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just("ip:" + remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("ip:unknown");
        };
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    @Bean
    public RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    // ============================================================
    // CORS
    // ============================================================

    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://your-frontend.com",
                "http://localhost:3000",
                "http://localhost:8080"
        ));
        config.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-EWM-USER-ID",
                "X-USER-ID",
                "X-USER-ROLE",
                "X-Correlation-Id"
        ));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS configuration applied");
        return new CorsWebFilter(source);
    }

    // ============================================================
    // ROUTES
    // ============================================================

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Configuring routes...");

        return builder.routes()
                // === AUTH SERVICE ===
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(ipKeyResolver())
                                        .setRateLimiter(authRateLimiter()))
                                .circuitBreaker(config -> config
                                        .setName("authServiceCB")
                                        .setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri("lb://auth-service"))

                // === USER SERVICE ===
                .route("user-service-public", r -> r
                        .path("/users/**")
                        .and().method(HttpMethod.GET)
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

                .route("user-service-admin", r -> r
                        .path("/admin/users/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .filter(roleValidationFilter)
                                .circuitBreaker(config -> config
                                        .setName("userServiceCB")
                                        .setFallbackUri("forward:/fallback/users"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(adminRateLimiter()))
                        )
                        .uri("lb://user-service"))

                // === EVENT SERVICE ===
                .route("event-service-public", r -> r
                        .path("/categories/**", "/compilations/**", "/events/**")
                        .and().method(HttpMethod.GET)
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("eventServiceCB")
                                        .setFallbackUri("forward:/fallback/events"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(ipKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://event-service"))

                .route("event-service-protected", r -> r
                        .path(
                                "/users/*/events/**",
                                "/admin/categories/**",
                                "/admin/compilations/**",
                                "/admin/events/**",
                                "/events/**/like"
                        )
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .filter(roleValidationFilter)
                                .circuitBreaker(config -> config
                                        .setName("eventServiceCB")
                                        .setFallbackUri("forward:/fallback/events"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://event-service"))

                // === REQUEST SERVICE ===
                .route("request-service", r -> r
                        .path("/users/*/requests/**", "/users/*/events/*/requests/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .filter(roleValidationFilter)
                                .circuitBreaker(config -> config
                                        .setName("requestServiceCB")
                                        .setFallbackUri("forward:/fallback/requests"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://request-service"))

                // === COMMENT SERVICE ===
                .route("comment-service-public", r -> r
                        .path("/comments/**")
                        .and().method(HttpMethod.GET)
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("commentServiceCB")
                                        .setFallbackUri("forward:/fallback/comments"))
                        )
                        .uri("lb://comment-service"))

                .route("comment-service-protected", r -> r
                        .path("/admin/comments/**", "/users/*/comments/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .filter(roleValidationFilter)
                                .circuitBreaker(config -> config
                                        .setName("commentServiceCB")
                                        .setFallbackUri("forward:/fallback/comments"))
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://comment-service"))

                // === STATS SERVICE ===
                .route("stats-server", r -> r
                        .path("/hit", "/stats")
                        .filters(f -> f
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(ipKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://stats-server"))

                // === GRAPHQL ===
                .route("graphql", r -> r
                        .path("/graphql")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter)
                                .requestRateLimiter(config -> config
                                        .setKeyResolver(userKeyResolver())
                                        .setRateLimiter(redisRateLimiter()))
                        )
                        .uri("lb://event-service"))
                .build();
    }
}