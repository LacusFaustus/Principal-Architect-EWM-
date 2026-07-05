package ru.practicum.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {
        Health.Builder status = Health.up();

        // Проверка Database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            status.withDetail("database", "available");
        } catch (Exception e) {
            log.error("Database health check failed", e);
            status.down().withDetail("database", "unavailable: " + e.getMessage());
        }

        // Проверка Kafka
        try {
            kafkaTemplate.send("health-check", "test").get();
            status.withDetail("kafka", "available");
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            status.down().withDetail("kafka", "unavailable: " + e.getMessage());
        }

        return status.build();
    }
}