package ru.practicum.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.auth.TestSecurityConfig;
import ru.practicum.auth.dto.LoginRequest;
import ru.practicum.auth.dto.LoginResponse;
import ru.practicum.auth.dto.RefreshTokenRequest;
import ru.practicum.auth.security.JwtTokenProvider;
import ru.practicum.auth.service.AuthService;
import ru.practicum.auth.service.TokenService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.class
        }
)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void login_ShouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(86400000L)
                .tokenType("Bearer")
                .userId(1L)
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void refreshToken_ShouldReturnNewToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        LoginResponse response = LoginResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("refresh-token")
                .expiresIn(86400000L)
                .tokenType("Bearer")
                .userId(1L)
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_ShouldReturnOk() throws Exception {
        doNothing().when(authService).logout("token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void validateToken_ShouldReturnTrue() throws Exception {
        when(authService.validateToken("valid-token")).thenReturn(true);

        mockMvc.perform(post("/api/auth/validate")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}