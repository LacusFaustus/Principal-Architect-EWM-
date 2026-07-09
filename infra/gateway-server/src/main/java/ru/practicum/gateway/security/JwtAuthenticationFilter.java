package ru.practicum.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {

    @Value("${jwt.secret:your-super-secret-jwt-key-at-least-32-characters-long}")
    private String jwtSecret;

    // Кэш для валидных токенов (чтобы не проверять каждый раз)
    private final Set<String> validTokenCache = ConcurrentHashMap.newKeySet();
    private static final long CACHE_TTL_MS = 300_000; // 5 минут

    // Публичные эндпоинты (не требуют авторизации)
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
            // Проверяем кэш
            if (!validTokenCache.contains(token)) {
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

                // Добавляем в кэш
                validTokenCache.add(token);
                log.info("Token validated and cached: userId={}, role={}", userId, role);
            }

            // Добавляем данные пользователя в заголовки
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-USER-ID", extractUserId(token))
                    .header("X-USER-EMAIL", extractEmail(token))
                    .header("X-USER-ROLE", extractRole(token))
                    .header("X-AUTHENTICATED", "true")
                    .header("X-Correlation-Id", generateCorrelationId())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Token-Error", "expired");
            return response.setComplete();
        } catch (SignatureException e) {
            log.warn("Invalid token signature: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Token-Error", "invalid");
            return response.setComplete();
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("X-Token-Error", "malformed");
            return response.setComplete();
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

    private String extractUserId(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.get("userId", String.class);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractEmail(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractRole(String token) {
        try {
            Claims claims = validateToken(token);
            String role = claims.get("role", String.class);
            return role != null ? role : "USER";
        } catch (Exception e) {
            return "USER";
        }
    }

    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isPublicEndpoint(String path) {
        if (PUBLIC_ENDPOINTS.stream().anyMatch(path::equals)) {
            return true;
        }
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    // Очистка кэша (периодическая)
    public void clearCache() {
        validTokenCache.clear();
        log.info("Token cache cleared");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}