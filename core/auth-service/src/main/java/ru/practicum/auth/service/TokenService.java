package ru.practicum.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.practicum.auth.model.RefreshToken;
import ru.practicum.auth.repository.RefreshTokenRepository;
import ru.practicum.auth.security.JwtTokenProvider;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;

    @Cacheable(value = "tokens", key = "#token")
    public boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        // Проверяем JWT валидность
        if (!tokenProvider.validateToken(token)) {
            log.debug("Token is invalid or expired: {}", token);
            return false;
        }

        // Для refresh токенов проверяем в БД
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElse(null);
        if (refreshToken != null) {
            if (refreshToken.getRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.debug("Refresh token is revoked or expired: {}", token);
                return false;
            }
        }

        return true;
    }

    @CacheEvict(value = "tokens", key = "#token")
    public void invalidateToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                    log.info("Token invalidated: {}", token);
                });
    }

    @CacheEvict(value = "tokens", allEntries = true)
    public void invalidateAllTokens() {
        log.info("All tokens invalidated");
    }
}