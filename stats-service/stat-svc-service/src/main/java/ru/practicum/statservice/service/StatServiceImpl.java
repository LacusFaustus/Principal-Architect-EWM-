package ru.practicum.statservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.mapper.EndpointHitMapper;
import ru.practicum.statservice.model.EndpointHit;
import ru.practicum.statservice.repository.StatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class StatServiceImpl implements StatService {
    private final StatRepository repository;
    private final EndpointHitMapper mapper;

    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        try {
            log.info("Saving hit: {}", hitDto);

            EndpointHit hit = mapper.mapToEndpointHit(hitDto);
            EndpointHit savedHit = repository.save(hit);

            log.info("Hit saved successfully: {}", savedHit);

        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения статистики: " + e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endTime = LocalDateTime.parse(end, formatter);
        List<ViewStatsDto> stats;

        List<String> urisParam = (uris == null || uris.isEmpty()) ? null : uris;

        if (unique) {
            stats = repository.findUniqueHits(startTime, endTime, urisParam);
        } else {
            stats = repository.findAllHits(startTime, endTime, urisParam);
        }

        return stats;
    }
}
