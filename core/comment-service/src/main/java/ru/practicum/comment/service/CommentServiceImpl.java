package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentLike;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentLikeRepository;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.dto.EventInfoDto;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.dto.UserShortInfoDto;
import ru.practicum.handler.exception.BadRequestException;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentMapper commentMapper;
    private final UserFeignClient userFeignClient;
    private final EventFeignClient eventFeignClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("Creating comment: userId={}, eventId={}", userId, eventId);

        checkUserExists(userId);
        checkEventExists(eventId);
        checkEventPublished(eventId);

        Comment comment = commentMapper.toComment(dto, userId, eventId);
        Comment savedComment = commentRepository.save(comment);

        log.info("Comment created with id={}", savedComment.getId());

        return buildCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        log.info("Updating comment: userId={}, commentId={}", userId, commentId);

        Comment comment = checkCommentExists(commentId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("User is not the author of this comment");
        }

        if (comment.getStatus() == CommentStatus.PUBLISHED) {
            throw new ConflictException("Cannot update published comment");
        }

        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment updated: commentId={}", updatedComment.getId());

        return buildCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Deleting comment: userId={}, commentId={}", userId, commentId);

        Comment comment = checkCommentExists(commentId);

        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("User is not the author of this comment");
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);

        log.info("Comment deleted: commentId={}", commentId);
    }

    @Override
    public CommentDto getComment(Long commentId) {
        log.info("Getting comment: commentId={}", commentId);

        Comment comment = checkCommentExists(commentId);

        if (comment.getStatus() != CommentStatus.PUBLISHED) {
            throw new NotFoundException("Comment not published");
        }

        return buildCommentDto(comment);
    }

    @Override
    public Page<CommentDto> getEventComments(Long eventId, Pageable pageable) {
        log.info("Getting comments for event: eventId={}", eventId);

        checkEventExists(eventId);

        Page<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        return comments.map(this::buildCommentDto);
    }

    @Override
    public Page<CommentDto> getUserComments(Long userId, Pageable pageable) {
        log.info("Getting comments for user: userId={}", userId);

        checkUserExists(userId);

        Page<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        return comments.map(this::buildCommentDto);
    }

    @Override
    public Page<CommentDto> getPendingComments(Pageable pageable) {
        log.info("Getting pending comments");

        Page<Comment> comments = commentRepository.findByEventIdAndStatus(null, CommentStatus.PENDING, pageable);

        return comments.map(this::buildCommentDto);
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, String statusStr) {
        log.info("Moderating comment: commentId={}, status={}", commentId, statusStr);

        Comment comment = checkCommentExists(commentId);

        CommentStatus newStatus;
        try {
            newStatus = CommentStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + statusStr);
        }

        comment.setStatus(newStatus);
        comment.setUpdated(LocalDateTime.now());

        Comment moderatedComment = commentRepository.save(comment);
        log.info("Comment moderated: commentId={}, newStatus={}", commentId, newStatus);

        return buildCommentDto(moderatedComment);
    }

    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        log.info("User likes comment: userId={}, commentId={}", userId, commentId);

        checkUserExists(userId);
        checkCommentExists(commentId);

        handleReaction(userId, commentId, true);
    }

    @Override
    @Transactional
    public void dislikeComment(Long userId, Long commentId) {
        log.info("User dislikes comment: userId={}, commentId={}", userId, commentId);

        checkUserExists(userId);
        checkCommentExists(commentId);

        handleReaction(userId, commentId, false);
    }

    @Override
    @Transactional
    public void removeReaction(Long userId, Long commentId) {
        log.info("User removes reaction: userId={}, commentId={}", userId, commentId);

        commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
        log.info("Reaction removed: userId={}, commentId={}", userId, commentId);
    }

    private void handleReaction(Long userId, Long commentId, boolean isLike) {
        CommentLike existing = commentLikeRepository.findByUserIdAndCommentId(userId, commentId).orElse(null);

        if (existing != null) {
            if (existing.getIsLike().equals(isLike)) {
                // Same reaction - remove it (toggle off)
                commentLikeRepository.delete(existing);
                log.info("Reaction removed (toggle off): userId={}, commentId={}", userId, commentId);
            } else {
                // Different reaction - update
                existing.setIsLike(isLike);
                existing.setCreated(LocalDateTime.now());
                commentLikeRepository.save(existing);
                log.info("Reaction changed: userId={}, commentId={}, newIsLike={}", userId, commentId, isLike);
            }
        } else {
            // New reaction
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setIsLike(isLike);
            like.setCreated(LocalDateTime.now());
            commentLikeRepository.save(like);
            log.info("Reaction added: userId={}, commentId={}, isLike={}", userId, commentId, isLike);
        }
    }

    private CommentDto buildCommentDto(Comment comment) {
        UserInfoDto author = getUserInfo(comment.getAuthorId());
        UserShortInfoDto authorShort = author != null ?
                new UserShortInfoDto(author.getId(), author.getName()) :
                new UserShortInfoDto(comment.getAuthorId(), "Unknown User");

        Long likes = commentLikeRepository.countLikesByCommentId(comment.getId());
        Long dislikes = commentLikeRepository.countDislikesByCommentId(comment.getId());

        CommentDto dto = commentMapper.toCommentDto(comment, authorShort, likes != null ? likes : 0L, dislikes != null ? dislikes : 0L);

        if (dto.getAuthor() != null && author != null && dto.getAuthor().getId() == null) {
            dto.getAuthor().setId(author.getId());
        }

        return dto;
    }

    private void checkUserExists(Long userId) {
        try {
            UserInfoDto user = userFeignClient.getUserById(userId);
            if (user == null) {
                log.warn("User {} not found", userId);
            }
        } catch (Exception e) {
            log.warn("User service unavailable for user {}: {}", userId, e.getMessage());
        }
    }

    private void checkEventExists(Long eventId) {
        try {
            EventInfoDto event = eventFeignClient.getEventById(eventId);
            if (event == null) {
                log.warn("Event {} not found", eventId);
            }
        } catch (Exception e) {
            log.warn("Event service unavailable for event {}: {}", eventId, e.getMessage());
        }
    }

    private void checkEventPublished(Long eventId) {
        try {
            EventInfoDto event = eventFeignClient.getEventById(eventId);
            if (event != null && !"PUBLISHED".equals(event.getState())) {
                throw new ConflictException("Cannot comment on unpublished event");
            }
        } catch (Exception e) {
            log.warn("Event service unavailable for event {}: {}", eventId, e.getMessage());
        }
    }

    private Comment checkCommentExists(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment {} not found", commentId);
                    return new NotFoundException("Comment with id=" + commentId + " was not found");
                });
    }

    private UserInfoDto getUserInfo(Long userId) {
        try {
            return userFeignClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Failed to get user info for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }
}