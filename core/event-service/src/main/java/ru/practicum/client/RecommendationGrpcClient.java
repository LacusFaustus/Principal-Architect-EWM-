package ru.practicum.client;

import com.google.protobuf.util.Timestamps;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.proto.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class RecommendationGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorClient;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerClient;

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

    public List<Long> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerClient.getRecommendationsForUser(request);
            List<Long> recommendations = new ArrayList<>();
            iterator.forEachRemaining(proto -> recommendations.add(proto.getEventId()));

            log.info("Received {} recommendations for user {}", recommendations.size(), userId);
            return recommendations;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting recommendations for user {}: status={}, description={}",
                    userId, e.getStatus().getCode(), e.getStatus().getDescription(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Failed to get recommendations for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    public List<Long> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerClient.getSimilarEvents(request);
            List<Long> similarEvents = new ArrayList<>();
            iterator.forEachRemaining(proto -> similarEvents.add(proto.getEventId()));

            log.info("Found {} similar events for eventId={}, userId={}", similarEvents.size(), eventId, userId);
            return similarEvents;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting similar events: status={}, description={}",
                    e.getStatus().getCode(), e.getStatus().getDescription(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Failed to get similar events: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public Double getInteractionsCount(long eventId) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addEventId(eventId)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerClient.getInteractionsCount(request);
            if (iterator.hasNext()) {
                double score = iterator.next().getScore();
                log.debug("Got interactions count for eventId={}: {}", eventId, score);
                return score;
            }
            return 0.0;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting interactions count for eventId={}: status={}, description={}",
                    eventId, e.getStatus().getCode(), e.getStatus().getDescription(), e);
            return 0.0;
        } catch (Exception e) {
            log.error("Failed to get interactions count for eventId={}: {}", eventId, e.getMessage(), e);
            return 0.0;
        }
    }
}