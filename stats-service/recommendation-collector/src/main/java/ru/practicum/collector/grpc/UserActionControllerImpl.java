package ru.practicum.collector.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionControllerGrpc;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received user action: userId={}, eventId={}, actionType={}",
                    request.getUserId(), request.getEventId(), request.getActionType());

            UserActionAvro avroMessage = convertToAvro(request);

            kafkaTemplate.send(userActionsTopic, String.valueOf(avroMessage.getEventId()), avroMessage);

            log.info("User action sent to Kafka: {}", avroMessage);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    private UserActionAvro convertToAvro(UserActionProto proto) {
        ActionTypeAvro actionType;
        switch (proto.getActionType()) {
            case ACTION_VIEW:
                actionType = ActionTypeAvro.VIEW;
                break;
            case ACTION_REGISTER:
                actionType = ActionTypeAvro.REGISTER;
                break;
            case ACTION_LIKE:
                actionType = ActionTypeAvro.LIKE;
                break;
            default:
                throw new IllegalArgumentException("Unknown action type: " + proto.getActionType());
        }

        long timestampMillis = proto.getTimestamp().getSeconds() * 1000 + proto.getTimestamp().getNanos() / 1000000;

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(actionType)
                .setTimestamp(timestampMillis)
                .build();
    }
}