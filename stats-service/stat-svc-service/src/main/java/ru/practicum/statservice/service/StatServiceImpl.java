package ru.practicum.statservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.mapper.EndpointHitMapper;
import ru.practicum.statservice.model.EndpointHit;
import ru.practicum.statservice.repository.StatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    private final StatRepository repository;
    private final EndpointHitMapper mapper;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public void saveHit(NewEndpointHitDto hitDto) {
        EndpointHit hit = mapper.mapToEndpointHit(hitDto);
        repository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end,
                                       List<String> uris, boolean unique) {

        try {
            // Конвертируем строки в LocalDateTime
            LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);

            // Вызываем соответствующие методы репозитория
            if (uris == null || uris.isEmpty()) {
                // Все URI
                if (unique) {
                    return repository.findUniqueHitsAll(startTime, endTime);
                } else {
                    return repository.findAllHitsAll(startTime, endTime);
                }
            } else {
                // Только указанные URI
                if (unique) {
                    return repository.findUniqueHitsByUris(startTime, endTime, uris);
                } else {
                    return repository.findAllHitsByUris(startTime, endTime, uris);
                }
            }

        } catch (Exception e) {
            // Логируем ошибку и возвращаем пустой список
            // В продакшене нужно обрабатывать ошибки лучше
            return Collections.emptyList();
        }
    }
}
