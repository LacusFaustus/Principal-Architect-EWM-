package ru.practicum.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private MockGatewayFilterChain chain;
    private String validToken;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();

        // Устанавливаем JWT_SECRET через ReflectionTestUtils
        String secret = "your-super-secret-jwt-key-at-least-32-characters-long";
        ReflectionTestUtils.setField(filter, "jwtSecret", secret);

        // Генерируем валидный JWT токен для тестов
        validToken = generateValidToken(secret);

        chain = new MockGatewayFilterChain();
    }

    /**
     * Генерирует валидный JWT токен для тестов
     */
    private String generateValidToken(String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "1");
        claims.put("role", "USER");

        return Jwts.builder()
                .claims(claims)
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1 час
                .signWith(key)
                .compact();
    }

    @Test
    void filter_PublicEndpoint_ShouldProceed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/auth/login")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertTrue(chain.isCalled());
    }

    @Test
    void filter_PublicEndpoint_Categories_ShouldProceed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/categories")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertTrue(chain.isCalled());
    }

    @Test
    void filter_PublicEndpoint_Compilations_ShouldProceed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/compilations")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertTrue(chain.isCalled());
    }

    @Test
    void filter_PublicEndpoint_Events_ShouldProceed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/events")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertTrue(chain.isCalled());
    }

    @Test
    void filter_ProtectedEndpoint_NoToken_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/admin/users")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertFalse(chain.isCalled());
    }

    @Test
    void filter_ProtectedEndpoint_InvalidToken_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/admin/users")
                .header("Authorization", "Bearer invalid_token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertFalse(chain.isCalled());
    }

    @Test
    void filter_ProtectedEndpoint_ValidToken_ShouldProceed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/admin/users")
                .header("Authorization", "Bearer " + validToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        // Проверяем, что цепочка была вызвана
        assertTrue(chain.isCalled());

        // Проверяем, что заголовки были добавлены
        assertTrue(exchange.getRequest().getHeaders().containsKey("X-USER-ID"));
        assertTrue(exchange.getRequest().getHeaders().containsKey("X-USER-EMAIL"));
        assertTrue(exchange.getRequest().getHeaders().containsKey("X-USER-ROLE"));
        assertTrue(exchange.getRequest().getHeaders().containsKey("X-AUTHENTICATED"));

        // Проверяем значения заголовков
        assertEquals("1", exchange.getRequest().getHeaders().getFirst("X-USER-ID"));
        assertEquals("test@example.com", exchange.getRequest().getHeaders().getFirst("X-USER-EMAIL"));
        assertEquals("USER", exchange.getRequest().getHeaders().getFirst("X-USER-ROLE"));
        assertEquals("true", exchange.getRequest().getHeaders().getFirst("X-AUTHENTICATED"));
    }

    @Test
    void filter_ProtectedEndpoint_ValidToken_MissingUserId_ShouldReturnUnauthorized() {
        // Arrange
        String secret = "your-super-secret-jwt-key-at-least-32-characters-long";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Токен без userId
        String tokenWithoutUserId = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/admin/users")
                .header("Authorization", "Bearer " + tokenWithoutUserId)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertFalse(chain.isCalled());
    }

    @Test
    void filter_ProtectedEndpoint_ExpiredToken_ShouldReturnUnauthorized() {
        // Arrange
        String secret = "your-super-secret-jwt-key-at-least-32-characters-long";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Просроченный токен
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", "1")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 часа назад
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 час назад
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/admin/users")
                .header("Authorization", "Bearer " + expiredToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertFalse(chain.isCalled());
    }

    @Test
    void filter_ProtectedEndpoint_BearerWithoutToken_ShouldReturnUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/admin/users")
                .header("Authorization", "Bearer ")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertFalse(chain.isCalled());
    }

    @Test
    void filter_ActuatorHealth_ShouldProceed() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/actuator/health")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        Mono<Void> result = filter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
        assertTrue(chain.isCalled());
    }

    static class MockGatewayFilterChain implements GatewayFilterChain {
        private boolean called = false;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            called = true;
            return Mono.empty();
        }

        public boolean isCalled() {
            return called;
        }
    }
}