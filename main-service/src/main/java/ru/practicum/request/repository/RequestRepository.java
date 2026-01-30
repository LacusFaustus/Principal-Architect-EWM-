package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestState;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByEventId(Long eventId);

    Integer countByEventIdAndStatus(Long eventId, RequestState status);

    List<Request> findByIdInAndStatus(List<Long> ids, RequestState status);
}
