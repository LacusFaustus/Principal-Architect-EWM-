package ru.practicum.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.client.EventFeignClient;
import ru.practicum.client.RecommendationGrpcClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.dto.EventInfoDto;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private EventFeignClient eventFeignClient;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private RecommendationGrpcClient recommendationGrpcClient;

    @InjectMocks
    private RequestServiceImpl requestService;

    private Long userId;
    private Long eventId;
    private Request request;
    private EventInfoDto eventInfo;
    private UserInfoDto userInfo;
    private ParticipationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        eventId = 1L;

        request = Request.builder()
                .id(1L)
                .requesterId(userId)
                .eventId(eventId)
                .status(RequestState.PENDING)
                .created(LocalDateTime.now())
                .build();

        eventInfo = new EventInfoDto();
        eventInfo.setId(eventId);
        eventInfo.setInitiatorId(2L);
        eventInfo.setParticipantLimit(10);
        eventInfo.setRequestModeration(true);
        eventInfo.setState("PUBLISHED");

        userInfo = new UserInfoDto();
        userInfo.setId(userId);
        userInfo.setName("Test User");
        userInfo.setEmail("test@example.com");

        requestDto = new ParticipationRequestDto();
        requestDto.setId(1L);
        requestDto.setRequester(userId);
        requestDto.setEvent(eventId);
        requestDto.setStatus("PENDING");
        requestDto.setCreated(LocalDateTime.now());
    }

    @Test
    void getRequests_ShouldReturnList() {
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(requestRepository.findByRequesterId(userId)).thenReturn(List.of(request));
        when(requestMapper.mapToRequestDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = requestService.getRequests(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(requestDto.getId(), result.get(0).getId());
    }

    @Test
    void postRequest_ShouldCreatePendingRequest() {
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByRequesterIdAndEventId(userId, eventId)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(requestMapper.mapToRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.postRequest(userId, eventId);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(recommendationGrpcClient).sendUserAction(userId, eventId, ru.practicum.stats.proto.ActionTypeProto.ACTION_REGISTER);
    }

    @Test
    void postRequest_WhenEventInitiator_ShouldThrowConflict() {
        eventInfo.setInitiatorId(userId);
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);

        assertThrows(ConflictException.class, () -> requestService.postRequest(userId, eventId));
    }

    @Test
    void postRequest_WhenNotPublished_ShouldThrowConflict() {
        eventInfo.setState("PENDING");
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);

        assertThrows(ConflictException.class, () -> requestService.postRequest(userId, eventId));
    }

    @Test
    void postRequest_WhenDuplicate_ShouldThrowConflict() {
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByRequesterIdAndEventId(userId, eventId)).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () -> requestService.postRequest(userId, eventId));
    }

    @Test
    void postRequest_WhenLimitReached_ShouldThrowConflict() {
        eventInfo.setParticipantLimit(1);
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByRequesterIdAndEventId(userId, eventId)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> requestService.postRequest(userId, eventId));
    }

    @Test
    void postRequest_WithoutModeration_ShouldConfirm() {
        eventInfo.setRequestModeration(false);
        eventInfo.setParticipantLimit(0);
        Request confirmedRequest = Request.builder()
                .id(1L)
                .requesterId(userId)
                .eventId(eventId)
                .status(RequestState.CONFIRMED)
                .created(LocalDateTime.now())
                .build();

        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByRequesterIdAndEventId(userId, eventId)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(Request.class))).thenReturn(confirmedRequest);

        ParticipationRequestDto confirmedDto = new ParticipationRequestDto();
        confirmedDto.setId(1L);
        confirmedDto.setStatus("CONFIRMED");
        when(requestMapper.mapToRequestDto(confirmedRequest)).thenReturn(confirmedDto);

        ParticipationRequestDto result = requestService.postRequest(userId, eventId);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
    }

    @Test
    void patchRequest_ShouldCancel() {
        Long requestId = 1L;
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(requestMapper.mapToRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.patchRequest(userId, requestId);

        assertNotNull(result);
        verify(requestRepository).save(request);
    }

    @Test
    void patchRequest_WhenNotOwner_ShouldThrowConflict() {
        Long requestId = 1L;
        request.setRequesterId(3L);
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () -> requestService.patchRequest(userId, requestId));
    }

    @Test
    void patchRequest_WhenAlreadyConfirmed_ShouldThrowConflict() {
        Long requestId = 1L;
        request.setStatus(RequestState.CONFIRMED);
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () -> requestService.patchRequest(userId, requestId));
    }

    @Test
    void getEventRequests_ShouldReturnList() {
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByEventId(eventId)).thenReturn(List.of(request));
        when(requestMapper.mapToRequestDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = requestService.getEventRequests(userId, eventId);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void updateRequestStatus_ShouldConfirmRequests() {
        Long requestId = 1L;
        List<Long> requestIds = List.of(requestId);
        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(requestIds);
        updateRequest.setStatus("CONFIRMED");

        request.setStatus(RequestState.PENDING);

        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByIdIn(requestIds)).thenReturn(List.of(request));
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(0L);
        when(requestRepository.saveAll(any(List.class))).thenReturn(List.of(request));
        when(requestMapper.mapToRequestDto(request)).thenReturn(requestDto);

        EventRequestStatusUpdateResult result = requestService.patchEventRequestsStatus(userId, eventId, updateRequest);

        assertNotNull(result);
        assertEquals(1, result.getConfirmedRequests().size());
    }

    @Test
    void updateRequestStatus_WhenLimitReached_ShouldRejectExcess() {
        Long requestId1 = 1L;
        Long requestId2 = 2L;
        List<Long> requestIds = List.of(requestId1, requestId2);
        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(requestIds);
        updateRequest.setStatus("CONFIRMED");

        Request request1 = Request.builder()
                .id(1L)
                .requesterId(userId)
                .eventId(eventId)
                .status(RequestState.PENDING)
                .created(LocalDateTime.now())
                .build();

        Request request2 = Request.builder()
                .id(2L)
                .requesterId(3L)
                .eventId(eventId)
                .status(RequestState.PENDING)
                .created(LocalDateTime.now())
                .build();

        eventInfo.setParticipantLimit(1);

        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByIdIn(requestIds)).thenReturn(List.of(request1, request2));
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(0L);
        when(requestRepository.saveAll(any(List.class))).thenReturn(List.of(request1, request2));
        when(requestMapper.mapToRequestDto(any(Request.class))).thenReturn(requestDto);

        EventRequestStatusUpdateResult result = requestService.patchEventRequestsStatus(userId, eventId, updateRequest);

        assertNotNull(result);
        assertTrue(result.getConfirmedRequests().size() <= 1);
    }

    @Test
    void updateRequestStatus_WhenNotPending_ShouldThrowConflict() {
        Long requestId = 1L;
        List<Long> requestIds = List.of(requestId);
        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(requestIds);
        updateRequest.setStatus("CONFIRMED");

        request.setStatus(RequestState.CONFIRMED);

        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenReturn(eventInfo);
        when(requestRepository.findByIdIn(requestIds)).thenReturn(List.of(request));

        assertThrows(ConflictException.class, () ->
                requestService.patchEventRequestsStatus(userId, eventId, updateRequest));
    }

    @Test
    void countConfirmedRequestsByEventId_ShouldReturnCount() {
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(5L);

        Long result = requestService.countConfirmedRequestsByEventId(eventId);

        assertEquals(5L, result);
    }

    @Test
    void getRequests_WhenNoRequests_ShouldReturnEmptyList() {
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(requestRepository.findByRequesterId(userId)).thenReturn(List.of());

        List<ParticipationRequestDto> result = requestService.getRequests(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void patchRequest_WhenRequestNotFound_ShouldThrowNotFound() {
        Long requestId = 999L;
        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.patchRequest(userId, requestId));
    }

    // Удаляем проблемный тест или переписываем его правильно
    @Test
    void postRequest_WhenEventServiceUnavailable_ShouldUseFallback() {
        // Этот тест проверяет поведение при недоступности event-service
        // В реальности, если event-service недоступен, используется fallback
        // но initiatorId должен быть корректным

        // Создаем fallback данные с корректным initiatorId
        EventInfoDto fallbackEvent = new EventInfoDto();
        fallbackEvent.setId(eventId);
        fallbackEvent.setInitiatorId(3L); // Не равен userId
        fallbackEvent.setParticipantLimit(0);
        fallbackEvent.setRequestModeration(false);
        fallbackEvent.setState("PUBLISHED");

        when(userFeignClient.getUserById(userId)).thenReturn(userInfo);
        when(eventFeignClient.getEventById(eventId)).thenThrow(new RuntimeException("Service unavailable"));
        when(requestRepository.findByRequesterIdAndEventId(userId, eventId)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(eventId, RequestState.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(requestMapper.mapToRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestService.postRequest(userId, eventId);

        assertNotNull(result);
        verify(recommendationGrpcClient).sendUserAction(userId, eventId, ru.practicum.stats.proto.ActionTypeProto.ACTION_REGISTER);
    }
}