package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    Long countByEventIdAndStatus(Long eventId, RequestState status);

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByRequesterId(Long requesterId);

    Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    List<Request> findAllByIdIn(List<Long> requestIds);

    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    Long countByEventIdAndStatusIn(Long eventId, List<RequestState> status);

    List<Request> findByIdIn(List<Long> ids);

    Optional<Request> findByRequesterIdAndEventId(Long requesterId, Long eventId);
}
