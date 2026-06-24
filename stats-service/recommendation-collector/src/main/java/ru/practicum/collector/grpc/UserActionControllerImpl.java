package ru.practicum.collector.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionControllerGrpc;
import ru.practicum.stats.proto.UserActionProto;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.user-actions:stats.user-actions.v1}")
    private String userActionsTopic;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received user action: userId={}, eventId={}, actionType={}",
                    request.getUserId(), request.getEventId(), request.getActionType());

            // Создаем JSON сообщение вместо Avro
            Map<String, Object> message = new HashMap<>();
            message.put("userId", request.getUserId());
            message.put("eventId", request.getEventId());
            message.put("actionType", request.getActionType().name());
            message.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(userActionsTopic, String.valueOf(request.getEventId()), message);

            log.info("User action sent to Kafka: {}", message);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }
}