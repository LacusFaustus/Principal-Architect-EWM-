package ru.practicum.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.saga.event.SagaEvent;
import ru.practicum.saga.event.SagaEventType;
import ru.practicum.saga.event.SagaStepEvent;
import ru.practicum.saga.exception.SagaExecutionException;
import ru.practicum.saga.exception.SagaRollbackException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, SagaExecution> activeSagas = new ConcurrentHashMap<>();

    private static final String SAGA_TOPIC = "saga.events.v1";

    /**
     * Запускает новую Saga
     */
    public String startSaga(String sagaType, Map<String, Object> context) {
        String sagaId = UUID.randomUUID().toString();

        SagaExecution saga = SagaExecution.builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .context(context)
                .status(SagaStatus.STARTED)
                .startedAt(LocalDateTime.now())
                .steps(new ArrayList<>())
                .build();

        activeSagas.put(sagaId, saga);

        // Отправляем событие старта
        SagaEvent event = SagaEvent.builder()
                .sagaId(sagaId)
                .eventType(SagaEventType.STARTED)
                .data(Map.of("sagaType", sagaType, "context", context))
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(SAGA_TOPIC, sagaId, event);
        log.info("Saga started: id={}, type={}", sagaId, sagaType);

        return sagaId;
    }

    /**
     * Выполняет шаг Saga
     */
    public void executeStep(String sagaId, SagaStep step, Map<String, Object> params) {
        SagaExecution saga = getSaga(sagaId);
        validateSagaStatus(saga);

        try {
            log.info("Executing saga step: sagaId={}, step={}", sagaId, step.getName());

            // Отправляем событие о начале шага
            SagaStepEvent stepEvent = SagaStepEvent.builder()
                    .sagaId(sagaId)
                    .stepName(step.getName())
                    .eventType(SagaEventType.STEP_STARTED)
                    .params(params)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(SAGA_TOPIC, sagaId, stepEvent);

            // Выполняем шаг
            SagaStepResult result = step.execute(params);

            if (result.isSuccess()) {
                // Шаг выполнен успешно
                SagaStepExecution stepExecution = SagaStepExecution.builder()
                        .stepName(step.getName())
                        .status(SagaStepStatus.COMPLETED)
                        .result(result.getData())
                        .completedAt(LocalDateTime.now())
                        .build();

                saga.getSteps().add(stepExecution);

                // Отправляем событие об успешном завершении
                SagaEvent successEvent = SagaEvent.builder()
                        .sagaId(sagaId)
                        .eventType(SagaEventType.STEP_COMPLETED)
                        .data(Map.of(
                                "stepName", step.getName(),
                                "result", result.getData()
                        ))
                        .timestamp(LocalDateTime.now())
                        .build();

                kafkaTemplate.send(SAGA_TOPIC, sagaId, successEvent);
                log.info("Saga step completed: sagaId={}, step={}", sagaId, step.getName());

            } else {
                // Шаг завершился с ошибкой - запускаем компенсацию
                log.error("Saga step failed: sagaId={}, step={}, error={}",
                        sagaId, step.getName(), result.getError());

                SagaEvent failEvent = SagaEvent.builder()
                        .sagaId(sagaId)
                        .eventType(SagaEventType.STEP_FAILED)
                        .data(Map.of(
                                "stepName", step.getName(),
                                "error", result.getError()
                        ))
                        .timestamp(LocalDateTime.now())
                        .build();

                kafkaTemplate.send(SAGA_TOPIC, sagaId, failEvent);
                rollback(sagaId, step.getName(), result.getError());
            }

        } catch (Exception e) {
            log.error("Error executing saga step: sagaId={}, step={}", sagaId, step.getName(), e);
            rollback(sagaId, step.getName(), e.getMessage());
        }
    }

    /**
     * Откатывает Saga
     */
    public void rollback(String sagaId, String failedStep, String error) {
        SagaExecution saga = getSaga(sagaId);

        log.warn("Rolling back saga: sagaId={}, failedStep={}, error={}",
                sagaId, failedStep, error);

        saga.setStatus(SagaStatus.ROLLBACK_IN_PROGRESS);
        saga.setError(error);

        // Отправляем событие о начале отката
        SagaEvent rollbackEvent = SagaEvent.builder()
                .sagaId(sagaId)
                .eventType(SagaEventType.ROLLBACK_STARTED)
                .data(Map.of("failedStep", failedStep, "error", error))
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(SAGA_TOPIC, sagaId, rollbackEvent);

        // Проходим по выполненным шагам в обратном порядке
        List<SagaStepExecution> completedSteps = new ArrayList<>(saga.getSteps());
        Collections.reverse(completedSteps);

        boolean compensationFailed = false;

        for (SagaStepExecution stepExecution : completedSteps) {
            if (stepExecution.getStatus() == SagaStepStatus.COMPLETED) {
                String stepName = stepExecution.getStepName();
                try {
                    log.info("Compensating step: sagaId={}, step={}", sagaId, stepName);

                    // Отправляем событие о компенсации
                    SagaEvent compensateEvent = SagaEvent.builder()
                            .sagaId(sagaId)
                            .eventType(SagaEventType.COMPENSATION_STARTED)
                            .data(Map.of("stepName", stepName))
                            .timestamp(LocalDateTime.now())
                            .build();

                    kafkaTemplate.send(SAGA_TOPIC, sagaId, compensateEvent);

                    // Выполняем компенсацию шага
                    compensateStep(stepName, stepExecution.getResult());

                    stepExecution.setStatus(SagaStepStatus.COMPENSATED);

                    SagaEvent compensateCompleteEvent = SagaEvent.builder()
                            .sagaId(sagaId)
                            .eventType(SagaEventType.COMPENSATION_COMPLETED)
                            .data(Map.of("stepName", stepName))
                            .timestamp(LocalDateTime.now())
                            .build();

                    kafkaTemplate.send(SAGA_TOPIC, sagaId, compensateCompleteEvent);
                    log.info("Step compensated: sagaId={}, step={}", sagaId, stepName);

                } catch (Exception e) {
                    log.error("Failed to compensate step: sagaId={}, step={}", sagaId, stepName, e);
                    compensationFailed = true;

                    SagaEvent compensateFailEvent = SagaEvent.builder()
                            .sagaId(sagaId)
                            .eventType(SagaEventType.COMPENSATION_FAILED)
                            .data(Map.of(
                                    "stepName", stepName,
                                    "error", e.getMessage()
                            ))
                            .timestamp(LocalDateTime.now())
                            .build();

                    kafkaTemplate.send(SAGA_TOPIC, sagaId, compensateFailEvent);

                    // В случае ошибки компенсации - продолжаем, но отмечаем это
                    saga.setCompensationError(e.getMessage());
                }
            }
        }

        if (compensationFailed) {
            saga.setStatus(SagaStatus.ROLLBACK_FAILED);
            throw new SagaRollbackException("Some compensation steps failed. Check saga details.");
        } else {
            saga.setStatus(SagaStatus.ROLLBACK_COMPLETED);
            saga.setCompletedAt(LocalDateTime.now());

            SagaEvent rollbackCompleteEvent = SagaEvent.builder()
                    .sagaId(sagaId)
                    .eventType(SagaEventType.ROLLBACK_COMPLETED)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(SAGA_TOPIC, sagaId, rollbackCompleteEvent);
            log.info("Saga rollback completed: sagaId={}", sagaId);
        }

        // Чистим завершенную сагу
        activeSagas.remove(sagaId);
    }

    /**
     * Завершает Saga успешно
     */
    public void completeSaga(String sagaId) {
        SagaExecution saga = getSaga(sagaId);
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCompletedAt(LocalDateTime.now());

        SagaEvent completeEvent = SagaEvent.builder()
                .sagaId(sagaId)
                .eventType(SagaEventType.COMPLETED)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(SAGA_TOPIC, sagaId, completeEvent);
        log.info("Saga completed: sagaId={}", sagaId);

        // Чистим завершенные саги через некоторое время
        activeSagas.remove(sagaId);
    }

    /**
     * Проверяет статус Saga
     */
    public SagaExecution getSagaStatus(String sagaId) {
        return getSaga(sagaId);
    }

    private SagaExecution getSaga(String sagaId) {
        SagaExecution saga = activeSagas.get(sagaId);
        if (saga == null) {
            throw new SagaExecutionException("Saga not found: " + sagaId);
        }
        return saga;
    }

    private void validateSagaStatus(SagaExecution saga) {
        if (saga.getStatus() == SagaStatus.ROLLBACK_IN_PROGRESS ||
                saga.getStatus() == SagaStatus.ROLLBACK_COMPLETED) {
            throw new SagaExecutionException("Cannot execute step in saga with rollback status");
        }
        if (saga.getStatus() == SagaStatus.COMPLETED) {
            throw new SagaExecutionException("Saga already completed");
        }
    }

    private void compensateStep(String stepName, Map<String, Object> data) {
        // Реализация компенсации для конкретных шагов
        switch (stepName) {
            case "create_user_profile":
                // Удаляем профиль пользователя из user-service
                Long userId = (Long) data.get("userId");
                if (userId != null) {
                    log.info("Compensating create_user_profile: deleting userId={}", userId);
                    // Здесь должен быть вызов userRepository.deleteById(userId)
                }
                break;
            case "create_user_credentials":
                // Удаляем учетные данные из auth-service
                userId = (Long) data.get("userId");
                if (userId != null) {
                    log.info("Compensating create_user_credentials: deleting userId={}", userId);
                    // Здесь должен быть вызов authServiceClient.deleteUserCredentials(userId)
                }
                break;
            case "create_event":
                // Удаляем событие
                Long eventId = (Long) data.get("eventId");
                if (eventId != null) {
                    log.info("Compensating create_event: deleting eventId={}", eventId);
                }
                break;
            default:
                log.warn("No compensation defined for step: {}", stepName);
        }
    }

    // Внутренние классы

    @lombok.Builder
    @lombok.Data
    public static class SagaExecution {
        private String sagaId;
        private String sagaType;
        private Map<String, Object> context;
        private SagaStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String error;
        private String compensationError;
        private List<SagaStepExecution> steps;
    }

    @lombok.Builder
    @lombok.Data
    public static class SagaStepExecution {
        private String stepName;
        private SagaStepStatus status;
        private Map<String, Object> result;
        private LocalDateTime completedAt;
    }

    public enum SagaStatus {
        STARTED,
        STEP_EXECUTING,
        COMPLETED,
        ROLLBACK_IN_PROGRESS,
        ROLLBACK_COMPLETED,
        ROLLBACK_FAILED
    }

    public enum SagaStepStatus {
        PENDING,
        COMPLETED,
        FAILED,
        COMPENSATED,
        COMPENSATION_FAILED
    }

    public interface SagaStep {
        String getName();
        SagaStepResult execute(Map<String, Object> params);
    }

    @lombok.Builder
    @lombok.Data
    public static class SagaStepResult {
        private boolean success;
        private Map<String, Object> data;
        private String error;

        public static SagaStepResult success(Map<String, Object> data) {
            return SagaStepResult.builder()
                    .success(true)
                    .data(data)
                    .build();
        }

        public static SagaStepResult failure(String error) {
            return SagaStepResult.builder()
                    .success(false)
                    .error(error)
                    .build();
        }
    }
}