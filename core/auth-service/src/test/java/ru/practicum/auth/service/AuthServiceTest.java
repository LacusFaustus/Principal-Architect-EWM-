package ru.practicum.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.practicum.auth.dto.LoginRequest;
import ru.practicum.auth.dto.LoginResponse;
import ru.practicum.auth.dto.RefreshTokenRequest;
import ru.practicum.auth.model.RefreshToken;
import ru.practicum.auth.model.Role;
import ru.practicum.auth.model.UserCredentials;
import ru.practicum.auth.repository.RefreshTokenRepository;
import ru.practicum.auth.repository.UserCredentialsRepository;
import ru.practicum.auth.security.JwtTokenProvider;
import ru.practicum.auth.security.UserDetailsServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserCredentialsRepository userCredentialsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private UserCredentials userCredentials;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        userCredentials = UserCredentials.builder()
                .id(1L)
                .userId(1L)
                .email("test@example.com")
                .password("encoded-password")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .roles(Set.of(Role.USER))
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                userCredentials, null, userCredentials.getAuthorities()
        );
    }

    @Test
    void login_ShouldReturnLoginResponse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(anyString())).thenReturn("refresh-token");
        when(userCredentialsRepository.findByEmail(anyString())).thenReturn(Optional.of(userCredentials));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userCredentialsRepository).save(any(UserCredentials.class));
    }

    @Test
    void refreshToken_ShouldReturnNewAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.extractEmail("refresh-token")).thenReturn("test@example.com");
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userCredentials);
        when(tokenProvider.generateToken(any(Authentication.class))).thenReturn("new-access-token");
        when(userCredentialsRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userCredentials));

        LoginResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void refreshToken_WithRevokedToken_ShouldThrowException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-token");

        RefreshToken refreshToken = RefreshToken.builder()
                .token("revoked-token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .build();

        when(tokenProvider.validateToken("revoked-token")).thenReturn(true);
        when(tokenProvider.extractEmail("revoked-token")).thenReturn("test@example.com");
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(refreshToken));

        assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
    }

    @Test
    void logout_ShouldRevokeToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("token")
                .userId(1L)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(refreshToken));

        authService.logout("token");

        assertTrue(refreshToken.getRevoked());
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void validateToken_ShouldReturnTrue() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        assertTrue(authService.validateToken("valid-token"));
    }

    @Test
    void validateToken_ShouldReturnFalse() {
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);
        assertFalse(authService.validateToken("invalid-token"));
    }

    @Test
    void createUser_ShouldReturnUserCredentials() {
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userCredentialsRepository.save(any(UserCredentials.class))).thenReturn(userCredentials);

        UserCredentials result = authService.createUser(1L, "test@example.com", "password");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encoded-password", result.getPassword());
    }
}