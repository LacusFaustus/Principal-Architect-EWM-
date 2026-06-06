package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.service.CommentService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/comments")
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}")
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto dto) {
        log.info("POST /users/{}/comments/events/{}", userId, eventId);
        CommentDto comment = commentService.createComment(userId, eventId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDto dto) {
        log.info("PATCH /users/{}/comments/{}", userId, commentId);
        CommentDto comment = commentService.updateComment(userId, commentId, dto);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        log.info("DELETE /users/{}/comments/{}", userId, commentId);
        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        log.info("GET /users/{}/comments/{}", userId, commentId);
        CommentDto comment = commentService.getComment(commentId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping
    public ResponseEntity<Page<CommentDto>> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /users/{}/comments", userId);
        PageRequest pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));
        Page<CommentDto> comments = commentService.getUserComments(userId, pageable);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        log.info("PUT /users/{}/comments/{}/like", userId, commentId);
        commentService.likeComment(userId, commentId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{commentId}/dislike")
    public ResponseEntity<Void> dislikeComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        log.info("PUT /users/{}/comments/{}/dislike", userId, commentId);
        commentService.dislikeComment(userId, commentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}/reaction")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        log.info("DELETE /users/{}/comments/{}/reaction", userId, commentId);
        commentService.removeReaction(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}