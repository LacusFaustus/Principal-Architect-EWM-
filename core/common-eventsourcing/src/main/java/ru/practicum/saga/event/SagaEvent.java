package ru.practicum.saga.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvent {
    private String sagaId;
    private SagaEventType eventType;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}