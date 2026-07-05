package ru.practicum;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.practicum.saga.SagaOrchestrator;

@TestConfiguration
public class TestApplicationConfig {

    @Bean
    @Primary
    public SagaOrchestrator sagaOrchestrator() {
        // Создаем SagaOrchestrator с mock KafkaTemplate
        return new SagaOrchestrator(null);
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}