package ru.practicum.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RoleValidationFilter implements GatewayFilter, Ordered {

    // Кэш для ролей (в реальном проекте можно заменить на Redis)
    private final Map<String, List<String>> roleCache = new ConcurrentHashMap<>();

    // Требуемые роли для разных путей
    private static final Map<String, String> PATH_ROLE_MAP = Map.of(
            "/admin/users", "ADMIN",
            "/admin/categories", "ADMIN",
            "/admin/compilations", "ADMIN",
            "/admin/events", "ADMIN",
            "/admin/comments", "ADMIN",
            "/users/*/events", "USER",
            "/users/*/requests", "USER",
            "/users/*/comments", "USER"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getPath().value();
        String role = request.getHeaders().getFirst("X-USER-ROLE");
        String userId = request.getHeaders().getFirst("X-USER-ID");

        // Проверяем, нужна ли валидация для этого пути
        String requiredRole = getRequiredRole(path);
        if (requiredRole == null) {
            return chain.filter(exchange);
        }

        // Если роль не указана - запрещаем
        if (role == null || role.isEmpty()) {
            log.warn("Access denied: no role provided for path: {}", path);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        // Проверяем роль
        if (!role.equals(requiredRole) && !role.equals("ADMIN")) {
            // ADMIN может делать все, остальные - только свою роль
            log.warn("Access denied: role={}, required={}, path={}", role, requiredRole, path);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        // Проверяем, что userId совпадает с тем, к чему пытается доступиться
        // (для эндпоинтов вида /users/{userId}/...)
        if (path.matches("/users/\\d+/.*") && !role.equals("ADMIN")) {
            String pathUserId = extractUserIdFromPath(path);
            if (pathUserId != null && !pathUserId.equals(userId)) {
                log.warn("Access denied: user {} trying to access resources of user {}",
                        userId, pathUserId);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }
        }

        log.debug("Access granted: role={}, path={}", role, path);
        return chain.filter(exchange);
    }

    private String getRequiredRole(String path) {
        // Точное совпадение
        if (PATH_ROLE_MAP.containsKey(path)) {
            return PATH_ROLE_MAP.get(path);
        }

        // Совпадение по шаблону
        for (Map.Entry<String, String> entry : PATH_ROLE_MAP.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.contains("*")) {
                String regex = pattern.replace("*", "\\d+");
                if (path.matches(regex)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private String extractUserIdFromPath(String path) {
        try {
            String[] parts = path.split("/");
            // /users/123/events -> parts[2] = 123
            if (parts.length >= 3 && "users".equals(parts[1])) {
                return parts[2];
            }
        } catch (Exception e) {
            log.warn("Failed to extract userId from path: {}", path);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return 0; // Выполняется после JwtAuthenticationFilter
    }
}