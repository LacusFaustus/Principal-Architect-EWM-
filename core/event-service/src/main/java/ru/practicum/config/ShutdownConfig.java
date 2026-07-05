package ru.practicum.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ShutdownConfig {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("Starting graceful shutdown...");

        // Завершаем все задачи
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.error("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Graceful shutdown completed");
    }
}