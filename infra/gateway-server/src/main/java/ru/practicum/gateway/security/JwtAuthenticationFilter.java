package ru.practicum.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {

    @Value("${jwt.secret:your-super-secret-jwt-key-at-least-32-characters-long}")
    private String jwtSecret;

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/validate",
            "/actuator/health",
            "/actuator/info"
    );

    private static final List<String> PUBLIC_PATHS = List.of(
            "/categories",
            "/compilations",
            "/events"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getPath().value();

        // Пропускаем публичные эндпоинты
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }

        // Проверяем наличие токена
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        String token = authHeader.substring(7);

        try {
            // Валидация токена
            Claims claims = validateToken(token);
            String userId = claims.get("userId", String.class);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null) {
                log.warn("Token missing userId claim");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            // Добавляем данные пользователя в заголовки для downstream сервисов
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-USER-ID", userId)
                    .header("X-USER-EMAIL", email != null ? email : "unknown")
                    .header("X-USER-ROLE", role != null ? role : "USER")
                    .header("X-AUTHENTICATED", "true")
                    .build();

            log.debug("Authenticated user: {} with role: {}", email, role);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isPublicEndpoint(String path) {
        // Проверяем точные совпадения
        if (PUBLIC_ENDPOINTS.stream().anyMatch(path::equals)) {
            return true;
        }

        // Проверяем паттерны
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}