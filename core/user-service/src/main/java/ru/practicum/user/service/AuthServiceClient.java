package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://auth-service:8086}")
    private String authServiceUrl;

    public void createUserCredentials(Long userId, String email, String password) {
        try {
            log.info("Creating auth credentials for user: {}", userId);

            String url = authServiceUrl + "/internal/auth/create";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("email", email);
            request.put("password", password);
            request.put("role", "USER");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Auth credentials created for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to create auth credentials for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create auth credentials", e);
        }
    }

    public void deleteUserCredentials(Long userId) {
        try {
            log.info("Deleting auth credentials for user: {}", userId);

            String url = authServiceUrl + "/internal/auth/" + userId;
            restTemplate.delete(url);

            log.info("Auth credentials deleted for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to delete auth credentials for user {}: {}", userId, e.getMessage());
            // Не выбрасываем исключение, так как это может быть не критично
        }
    }
}