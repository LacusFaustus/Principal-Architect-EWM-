package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findByUserIdOrderByLastActionTimeDesc(Long userId);

    @Query("SELECT u.eventId FROM UserAction u WHERE u.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(u.weight) FROM UserAction u WHERE u.eventId = :eventId")
    Double sumWeightsByEventId(@Param("eventId") Long eventId);

    @Query(value = "SELECT * FROM user_actions u WHERE u.user_id = :userId ORDER BY u.last_action_time DESC LIMIT :limit",
            nativeQuery = true)
    List<UserAction> findTopByUserIdOrderByLastActionTimeDesc(@Param("userId") Long userId, @Param("limit") int limit);
}