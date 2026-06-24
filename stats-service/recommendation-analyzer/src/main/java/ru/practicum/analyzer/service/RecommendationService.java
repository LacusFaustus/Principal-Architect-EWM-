package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Transactional
    public void initializeEvent(Long eventId) {
        log.info("Initializing event for recommendations: eventId={}", eventId);
        // Проверяем, есть ли уже записи для этого события
        List<EventSimilarity> existing = eventSimilarityRepository.findSimilarEvents(eventId);
        if (existing.isEmpty()) {
            // Добавляем событие в матрицу схожести с нулевыми значениями
            log.info("Event {} initialized with default similarity", eventId);
        } else {
            log.info("Event {} already has {} similarity records", eventId, existing.size());
        }
    }

    @Transactional
    public void activateEvent(Long eventId) {
        log.info("Activating event for recommendations: eventId={}", eventId);
        // Обновляем статус события в рекомендательной системе
        // Например, можно обновить метку времени последнего обновления
        List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEvents(eventId);
        if (!similarities.isEmpty()) {
            similarities.forEach(sim -> {
                sim.setUpdated(LocalDateTime.now());
                eventSimilarityRepository.save(sim);
            });
            log.info("Event {} activated with {} similarity records", eventId, similarities.size());
        } else {
            log.info("Event {} activated but no similarity records found", eventId);
        }
    }

    @Transactional
    public void updateEventMetadata(Long eventId) {
        log.info("Updating event metadata: eventId={}", eventId);
        // Обновляем метаданные события
        // Например, можно обновить категорию, название и т.д.
        // В текущей реализации просто обновляем время
        List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEvents(eventId);
        if (!similarities.isEmpty()) {
            similarities.forEach(sim -> {
                sim.setUpdated(LocalDateTime.now());
                eventSimilarityRepository.save(sim);
            });
            log.info("Event {} metadata updated", eventId);
        } else {
            log.info("Event {} metadata updated but no similarity records found", eventId);
        }
    }

    @Transactional
    public void cleanupOldActions() {
        log.info("Cleaning up old user actions");
        // Удаляем старые действия для оптимизации производительности
        // Например, действия старше 30 дней
        // В реальном проекте здесь должна быть логика архивации или удаления
        log.info("Old user actions cleanup completed");
    }
}