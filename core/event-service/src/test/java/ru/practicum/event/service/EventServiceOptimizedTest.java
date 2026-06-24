package ru.practicum.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.StatFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceOptimizedTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private StatFeignClient statFeignClient;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private RequestFeignClient requestFeignClient;

    @InjectMocks
    private EventServiceOptimized eventService;

    private List<Event> events;
    private List<EventShortDto> expectedDtos;

    @BeforeEach
    void setUp() {
        Event event1 = new Event();
        event1.setId(1L);
        event1.setInitiatorId(1L);

        Event event2 = new Event();
        event2.setId(2L);
        event2.setInitiatorId(1L);

        events = Arrays.asList(event1, event2);

        expectedDtos = Arrays.asList(
                new EventShortDto(1L, "Event 1", null, 0L, null, null, false, "Title 1", 0.0),
                new EventShortDto(2L, "Event 2", null, 0L, null, null, false, "Title 2", 0.0)
        );
    }

    @Test
    void getEventsOptimized_Success() {
        // Arrange
        UserInfoDto userInfo = new UserInfoDto();
        userInfo.setId(1L);
        userInfo.setName("Test User");

        when(userFeignClient.getUsersByIds(anyList())).thenReturn(List.of(userInfo));
        when(statFeignClient.getStats(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(List.of(
                        new ViewStatsDto("app", "/events/1", 10L),
                        new ViewStatsDto("app", "/events/2", 20L)
                ));
        when(requestFeignClient.countConfirmedRequestsByEventId(anyLong()))
                .thenReturn(5L, 3L);

        // Act
        List<EventShortDto> result = eventService.getEventsOptimized(events);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getEventsOptimized_EmptyList() {
        // Act
        List<EventShortDto> result = eventService.getEventsOptimized(Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getEventsOptimized_WithUserServiceFailure() {
        // Arrange
        when(userFeignClient.getUsersByIds(anyList()))
                .thenThrow(new RuntimeException("User service unavailable"));

        // Act
        List<EventShortDto> result = eventService.getEventsOptimized(events);

        // Assert
        assertNotNull(result);
        // Должен быть обработан fallback
    }

    @Test
    void getEventsOptimized_WithStatsServiceFailure() {
        // Arrange
        UserInfoDto userInfo = new UserInfoDto();
        userInfo.setId(1L);
        userInfo.setName("Test User");

        when(userFeignClient.getUsersByIds(anyList())).thenReturn(List.of(userInfo));
        when(statFeignClient.getStats(anyString(), anyString(), anyList(), anyBoolean()))
                .thenThrow(new RuntimeException("Stats service unavailable"));

        // Act
        List<EventShortDto> result = eventService.getEventsOptimized(events);

        // Assert
        assertNotNull(result);
        // Должен быть обработан fallback с нулевыми просмотрами
    }
}