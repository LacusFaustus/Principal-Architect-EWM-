package ru.practicum.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.auth.dto.LoginRequest;
import ru.practicum.auth.dto.LoginResponse;
import ru.practicum.auth.dto.RefreshTokenRequest;
import ru.practicum.auth.model.RefreshToken;
import ru.practicum.auth.model.UserCredentials;
import ru.practicum.auth.repository.RefreshTokenRepository;
import ru.practicum.auth.repository.UserCredentialsRepository;
import ru.practicum.auth.security.JwtTokenProvider;
import ru.practicum.auth.security.UserDetailsServiceImpl;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails.getUsername());

        UserCredentials user = userCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save refresh token
        RefreshToken tokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getUserId())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(tokenEntity);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userCredentialsRepository.save(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getExpirationMs())
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRoles().toString())
                .build();
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = tokenProvider.extractEmail(refreshToken);
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (tokenEntity.getRevoked() || tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = tokenProvider.generateToken(authentication);

        UserCredentials user = userCredentialsRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getExpirationMs())
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRoles().toString())
                .build();
    }

    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                    log.info("Token revoked: {}", token);
                });
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    public UserCredentials createUser(Long userId, String email, String password) {
        UserCredentials user = UserCredentials.builder()
                .userId(userId)
                .email(email)
                .password(passwordEncoder.encode(password))
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .failedAttempts(0)
                .build();
        return userCredentialsRepository.save(user);
    }
}