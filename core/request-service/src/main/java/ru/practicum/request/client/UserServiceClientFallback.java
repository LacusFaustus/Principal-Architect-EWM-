package ru.practicum.request.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public Object getUserById(Long userId) {
        log.warn("Fallback: User service unavailable for getUserById({})", userId);
        return null;
    }
}