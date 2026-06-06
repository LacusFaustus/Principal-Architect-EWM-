package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.AdminUpdateCommentDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.service.CommentService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping("/pending")
    public ResponseEntity<Page<CommentDto>> getPendingComments(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /admin/comments/pending");
        PageRequest pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "created"));
        Page<CommentDto> comments = commentService.getPendingComments(pageable);
        return ResponseEntity.ok(comments);
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentDto> moderateComment(
            @PathVariable Long commentId,
            @RequestBody AdminUpdateCommentDto dto) {
        log.info("PATCH /admin/comments/{}", commentId);
        CommentDto comment = commentService.moderateComment(commentId, dto.getStatus());
        return ResponseEntity.ok(comment);
    }
}