package ru.practicum.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto);

    void deleteComment(Long userId, Long commentId);

    CommentDto getComment(Long commentId);

    Page<CommentDto> getEventComments(Long eventId, Pageable pageable);

    Page<CommentDto> getUserComments(Long userId, Pageable pageable);

    // Admin methods
    Page<CommentDto> getPendingComments(Pageable pageable);

    CommentDto moderateComment(Long commentId, String status);

    // Like/Dislike methods
    void likeComment(Long userId, Long commentId);

    void dislikeComment(Long userId, Long commentId);

    void removeReaction(Long userId, Long commentId);
}