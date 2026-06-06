package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.stats.proto.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            Long userId = request.getUserId();
            int maxResults = request.getMaxResults();

            log.info("Getting recommendations for user: userId={}, maxResults={}", userId, maxResults);

            // 1. Получаем последние взаимодействия пользователя (ограничиваем maxResults)
            List<UserAction> userActions = userActionRepository.findTopByUserIdOrderByLastActionTimeDesc(userId, maxResults);

            if (userActions.isEmpty()) {
                log.info("No user actions found for userId={}", userId);
                responseObserver.onCompleted();
                return;
            }

            log.info("Found {} recent user actions for userId={}", userActions.size(), userId);

            // 2. Собираем ID мероприятий, с которыми пользователь уже взаимодействовал
            Set<Long> interactedEvents = userActionRepository.findEventIdsByUserId(userId);
            log.info("User {} has interacted with {} events", userId, interactedEvents.size());

            // 3. Для каждого действия ищем похожие мероприятия
            Map<Long, Double> candidateScores = new HashMap<>();

            for (UserAction action : userActions) {
                List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsOrderByScoreDesc(action.getEventId());

                for (EventSimilarity sim : similarities) {
                    Long candidateId = sim.getEventA().equals(action.getEventId()) ? sim.getEventB() : sim.getEventA();

                    // Исключаем уже взаимодействованные мероприятия
                    if (!interactedEvents.contains(candidateId)) {
                        candidateScores.merge(candidateId, sim.getScore(), Double::sum);
                    }
                }
            }

            log.info("Found {} candidate events for recommendations", candidateScores.size());

            // 4. Сортируем и отправляем результат
            List<RecommendedEventProto> recommendations = candidateScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(maxResults)
                    .map(entry -> RecommendedEventProto.newBuilder()
                            .setEventId(entry.getKey())
                            .setScore(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            log.info("Sending {} recommendations to user {}", recommendations.size(), userId);

            for (RecommendedEventProto recommendation : recommendations) {
                responseObserver.onNext(recommendation);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting recommendations for user", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            Long eventId = request.getEventId();
            Long userId = request.getUserId();
            int maxResults = request.getMaxResults();

            log.info("Getting similar events: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

            // Получаем мероприятия, которые пользователь уже видел (если userId > 0)
            Set<Long> seenEvents = userId > 0 ?
                    userActionRepository.findEventIdsByUserId(userId) : new HashSet<>();

            log.info("User {} has seen {} events", userId, seenEvents.size());

            // Ищем похожие мероприятия
            List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsOrderByScoreDesc(eventId);

            log.info("Found {} similar events for eventId={}", similarities.size(), eventId);

            List<RecommendedEventProto> similarEvents = similarities.stream()
                    .map(sim -> {
                        Long similarId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                        return Map.entry(similarId, sim.getScore());
                    })
                    .filter(entry -> !seenEvents.contains(entry.getKey()))
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(maxResults)
                    .map(entry -> RecommendedEventProto.newBuilder()
                            .setEventId(entry.getKey())
                            .setScore(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            log.info("Sending {} similar events to user", similarEvents.size());

            for (RecommendedEventProto similarEvent : similarEvents) {
                responseObserver.onNext(similarEvent);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting similar events", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<Long> eventIds = request.getEventIdList();

            log.info("Getting interactions count for {} events", eventIds.size());

            for (Long eventId : eventIds) {
                Double totalWeight = userActionRepository.sumWeightsByEventId(eventId);
                double score = totalWeight != null ? totalWeight : 0.0;

                log.debug("EventId={} total weight={}", eventId, score);

                responseObserver.onNext(RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(score)
                        .build());
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error getting interactions count", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }
}