package ru.practicum.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.dto.UserShortInfoDto;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "created", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updated", ignore = true)
    Comment toComment(NewCommentDto dto, Long authorId, Long eventId);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "eventId", source = "comment.eventId")
    @Mapping(target = "text", source = "comment.text")
    @Mapping(target = "status", expression = "java(comment.getStatus() != null ? comment.getStatus().name() : null)")
    @Mapping(target = "created", source = "comment.created")
    @Mapping(target = "updated", source = "comment.updated")
    @Mapping(target = "likes", source = "likes")
    @Mapping(target = "dislikes", source = "dislikes")
    CommentDto toCommentDto(Comment comment, UserShortInfoDto author, Long likes, Long dislikes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", expression = "java(java.time.LocalDateTime.now())")
    void updateCommentFromDto(UpdateCommentDto dto, @MappingTarget Comment comment);
}