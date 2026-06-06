package ru.practicum.comment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.CommentLike;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.commentId = :commentId AND cl.isLike = true")
    Long countLikesByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.commentId = :commentId AND cl.isLike = false")
    Long countDislikesByCommentId(@Param("commentId") Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);
}