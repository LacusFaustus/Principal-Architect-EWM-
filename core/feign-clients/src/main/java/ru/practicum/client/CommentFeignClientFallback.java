package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.CommentDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class CommentFeignClientFallback implements CommentFeignClient {

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        log.warn("Fallback: Comment service unavailable for getEventComments({})", eventId);
        return Collections.emptyList();
    }
}