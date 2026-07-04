package ru.practicum.statservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.mapper.EndpointHitMapper;
import ru.practicum.statservice.model.EndpointHit;
import ru.practicum.statservice.repository.StatRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatServiceImplTest {

    @Mock
    private StatRepository repository;

    @Mock
    private EndpointHitMapper mapper;

    @InjectMocks
    private StatServiceImpl statService;

    private NewEndpointHitDto hitDto;
    private EndpointHit endpointHit;

    @BeforeEach
    void setUp() {
        hitDto = NewEndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        endpointHit = new EndpointHit();
        endpointHit.setId(1L);
        endpointHit.setApp("test-app");
        endpointHit.setUri("/test");
        endpointHit.setIp("127.0.0.1");
        endpointHit.setTimestamp(LocalDateTime.now());
    }

    @Test
    void saveHit_ShouldSave() {
        when(mapper.mapToEndpointHit(any(NewEndpointHitDto.class))).thenReturn(endpointHit);
        when(repository.save(any(EndpointHit.class))).thenReturn(endpointHit);

        statService.saveHit(hitDto);

        verify(repository).save(any(EndpointHit.class));
    }

    @Test
    void getStats_WithUris_ShouldReturnStats() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-12-31 23:59:59";
        List<String> uris = List.of("/test");
        boolean unique = false;

        ViewStatsDto stats = new ViewStatsDto("test-app", "/test", 10L);
        when(repository.findAllHitsByUris(any(), any(), any())).thenReturn(List.of(stats));

        List<ViewStatsDto> result = statService.getStats(start, end, uris, unique);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/test", result.get(0).getUri());
        assertEquals(10L, result.get(0).getHits());
    }

    @Test
    void getStats_WithoutUris_ShouldReturnAllStats() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-12-31 23:59:59";
        boolean unique = false;

        ViewStatsDto stats = new ViewStatsDto("test-app", "/test", 10L);
        when(repository.findAllHitsAll(any(), any())).thenReturn(List.of(stats));

        List<ViewStatsDto> result = statService.getStats(start, end, null, unique);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getStats_WithUniqueTrue_ShouldReturnUniqueStats() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-12-31 23:59:59";
        List<String> uris = List.of("/test");
        boolean unique = true;

        ViewStatsDto stats = new ViewStatsDto("test-app", "/test", 5L);
        when(repository.findUniqueHitsByUris(any(), any(), any())).thenReturn(List.of(stats));

        List<ViewStatsDto> result = statService.getStats(start, end, uris, unique);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getHits());
    }

    @Test
    void getStats_WithInvalidDate_ShouldReturnEmptyList() {
        String start = "invalid-date";
        String end = "2024-12-31 23:59:59";

        List<ViewStatsDto> result = statService.getStats(start, end, null, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}