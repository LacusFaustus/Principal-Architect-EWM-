package ru.practicum.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatClientConfig {

    @Bean
    public StatClient statClient() {
        return new StatClient("http://localhost:9090");
    }
}
