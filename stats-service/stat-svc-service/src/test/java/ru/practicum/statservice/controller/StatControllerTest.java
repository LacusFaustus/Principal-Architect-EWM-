package ru.practicum.statservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.handler.ServiceHandler;
import ru.practicum.statservice.service.StatService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StatControllerTest {

    @Mock
    private StatService statService;

    @InjectMocks
    private StatController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ServiceHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void hit_ShouldReturnCreated() throws Exception {
        NewEndpointHitDto hitDto = NewEndpointHitDto.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        doNothing().when(statService).saveHit(any(NewEndpointHitDto.class));

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitDto)))
                .andExpect(status().isCreated());
    }

    @Test
    void hit_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Используем только строковые поля, без timestamp
        String json = "{\"app\":\"\",\"uri\":\"/test\",\"ip\":\"127.0.0.1\"}";

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_ShouldReturnStats() throws Exception {
        String start = "2024-01-01 00:00:00";
        String end = "2024-12-31 23:59:59";

        ViewStatsDto stats = new ViewStatsDto("test-app", "/test", 10L);
        when(statService.getStats(anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(List.of(stats));

        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].uri").value("/test"))
                .andExpect(jsonPath("$[0].hits").value(10L));
    }

    @Test
    void getStats_WithInvalidDate_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "invalid-date")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_WithStartAfterEnd_ShouldReturnBadRequest() throws Exception {
        String start = "2025-01-01 00:00:00";
        String end = "2024-12-31 23:59:59";

        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end))
                .andExpect(status().isBadRequest());
    }
}