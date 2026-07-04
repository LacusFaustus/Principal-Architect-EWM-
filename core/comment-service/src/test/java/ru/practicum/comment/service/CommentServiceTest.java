package ru.practicum.comment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.client.EventFeignClient;
import ru.practicum.client.UserFeignClient;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentStatus;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.dto.UserInfoDto;
import ru.practicum.dto.UserShortInfoDto;
import ru.practicum.handler.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private EventFeignClient eventFeignClient;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;
    private CommentDto commentDto;
    private NewCommentDto newCommentDto;
    private UserInfoDto userInfo;

    @BeforeEach
    void setUp() {
        comment = new Comment();
        comment.setId(1L);
        comment.setAuthorId(1L);
        comment.setEventId(1L);
        comment.setText("Great event!");
        comment.setStatus(CommentStatus.PENDING);
        comment.setCreated(LocalDateTime.now());

        commentDto = new CommentDto();
        commentDto.setId(1L);
        commentDto.setEventId(1L);
        commentDto.setText("Great event!");
        commentDto.setStatus("PENDING");

        newCommentDto = new NewCommentDto();
        newCommentDto.setText("Great event!");

        userInfo = new UserInfoDto();
        userInfo.setId(1L);
        userInfo.setName("Test User");
        userInfo.setEmail("test@example.com");
    }

    @Test
    void createComment_ShouldReturnCommentDto() {
        when(userFeignClient.getUserById(anyLong())).thenReturn(userInfo);
        when(commentMapper.toComment(any(), anyLong(), anyLong())).thenReturn(comment);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(commentMapper.toCommentDto(any(), any(), anyLong(), anyLong())).thenReturn(commentDto);

        CommentDto result = commentService.createComment(1L, 1L, newCommentDto);

        assertNotNull(result);
        assertEquals("Great event!", result.getText());
    }

    @Test
    void getComment_ShouldReturnCommentDto() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userFeignClient.getUserById(anyLong())).thenReturn(userInfo);
        when(commentMapper.toCommentDto(any(), any(), anyLong(), anyLong())).thenReturn(commentDto);

        CommentDto result = commentService.getComment(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getComment_NotFound_ThrowsNotFoundException() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.getComment(999L));
    }

    @Test
    void deleteComment_ShouldSetDeletedStatus() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.deleteComment(1L, 1L);

        assertEquals(CommentStatus.DELETED, comment.getStatus());
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_NotAuthor_ThrowsException() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(Exception.class, () -> commentService.deleteComment(2L, 1L));
    }
}