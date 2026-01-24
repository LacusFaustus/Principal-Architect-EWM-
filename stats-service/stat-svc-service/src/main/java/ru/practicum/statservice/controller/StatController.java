package ru.practicum.statservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.NewEndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statservice.handler.BadRequestException;
import ru.practicum.statservice.service.StatService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatController {
    private final StatService statService;

    @PostMapping("/hit")
    public ResponseEntity<Void> hit(@Valid @RequestBody NewEndpointHitDto hitDto) {
        log.info("Получен запрос на сохранение hit: {}", hitDto);
        statService.saveHit(hitDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ViewStatsDto>> getStats(
            @NotBlank @RequestParam String start,
            @NotBlank @RequestParam String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique) throws BadRequestException {

        validateDateRangeParams(start, end);
        log.info("Получен запрос на статистику: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);
        return ResponseEntity.ok().body(statService.getStats(start, end, uris, unique));

    }

    private void validateDateRangeParams(String start, String end) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startDate = LocalDateTime.parse(start, formatter);
            LocalDateTime endDate = LocalDateTime.parse(end, formatter);

            if (startDate.isAfter(endDate)) {
                throw new BadRequestException("Время начала не может быть после окончания выборки");
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Неверный формат даты. Используйте формат: yyyy-MM-dd HH:mm:ss");
        }
    }
}
