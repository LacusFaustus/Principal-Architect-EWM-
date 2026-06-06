package ru.practicum.client;

import com.google.protobuf.util.Timestamps;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionControllerGrpc;
import ru.practicum.stats.proto.UserActionProto;

@Slf4j
@Component
public class RecommendationGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorClient;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        try {
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                    .build();

            collectorClient.collectUserAction(request);
            log.info("Sent user action to collector: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error sending user action to collector: status={}, description={}",
                    e.getStatus().getCode(), e.getStatus().getDescription(), e);
        } catch (Exception e) {
            log.error("Failed to send user action to collector: {}", e.getMessage(), e);
        }
    }
}