package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private UserShortInfoDto author;
    private Long eventId;
    private String text;
    private String status;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Long likes;
    private Long dislikes;
}