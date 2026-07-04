package ru.practicum.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.RecommendationGrpcClient;
import ru.practicum.client.RequestFeignClient;
import ru.practicum.client.StatFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private StatFeignClient statFeignClient;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private RequestFeignClient requestFeignClient;

    @Mock
    private RecommendationGrpcClient recommendationGrpcClient;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event event;
    private Category category;
    private NewEventDto newEventDto;
    private UserInfoDto userInfo;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Concert");

        event = new Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setAnnotation("Test annotation for event");
        event.setDescription("Test description for event");
        event.setCategory(category);
        event.setInitiatorId(1L);
        event.setState(EventState.PENDING);
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setCreatedOn(LocalDateTime.now());

        newEventDto = new NewEventDto();
        newEventDto.setTitle("Test Event");
        newEventDto.setAnnotation("Test annotation for event");
        newEventDto.setDescription("Test description for event");
        newEventDto.setCategory(1L);
        newEventDto.setEventDate(LocalDateTime.now().plusDays(7));
        newEventDto.setPaid(false);
        newEventDto.setParticipantLimit(100);

        userInfo = new UserInfoDto();
        userInfo.setId(1L);
        userInfo.setName("Test User");
        userInfo.setEmail("test@example.com");
    }

    @Test
    void postEvent_ShouldReturnEventFullDto() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(category));
        when(eventMapper.toEvent(any(), any(), anyLong())).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(userFeignClient.getUserById(anyLong())).thenReturn(userInfo);

        var result = eventService.postEvent(1L, newEventDto);

        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void postEvent_CategoryNotFound_ThrowsNotFoundException() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.postEvent(1L, newEventDto));
    }

    @Test
    void getEventById_ShouldReturnEventFullDto() {
        when(eventRepository.findById(anyLong())).thenReturn(Optional.of(event));
        when(userFeignClient.getUserById(anyLong())).thenReturn(userInfo);
        when(requestFeignClient.countConfirmedRequestsByEventId(anyLong())).thenReturn(0L);

        var result = eventService.getEventById(1L, 1L, mock(HttpServletRequest.class));

        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
    }

    @Test
    void getEventById_NotFound_ThrowsNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.getEventById(999L, 1L, mock(HttpServletRequest.class)));
    }

    @Test
    void getEvents_ShouldReturnList() {
        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.findAllByInitiatorId(anyLong(), any(PageRequest.class))).thenReturn(page);
        when(userFeignClient.getUserById(anyLong())).thenReturn(userInfo);

        var result = eventService.getEvents(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}