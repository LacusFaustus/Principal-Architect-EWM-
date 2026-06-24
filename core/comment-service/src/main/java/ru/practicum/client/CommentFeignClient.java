package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.CommentDto;

import java.util.List;

@FeignClient(name = "comment-service", fallback = CommentFeignClientFallback.class)
public interface CommentFeignClient {

    @GetMapping("/comments/events/{eventId}")
    List<CommentDto> getEventComments(
            @PathVariable("eventId") Long eventId,
            @RequestParam(value = "from", defaultValue = "0") int from,
            @RequestParam(value = "size", defaultValue = "10") int size
    );
}