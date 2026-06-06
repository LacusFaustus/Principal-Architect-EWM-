package ru.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.eventId IN :eventIds AND c.status = :status")
    List<Comment> findByEventIdInAndStatus(@Param("eventIds") List<Long> eventIds, @Param("status") CommentStatus status);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.eventId = :eventId AND c.status = :status")
    Long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") CommentStatus status);
}