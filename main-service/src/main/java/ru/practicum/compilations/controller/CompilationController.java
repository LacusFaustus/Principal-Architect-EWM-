package ru.practicum.compilations.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.CompilationSearchParam;
import ru.practicum.compilations.service.CompilationService;

import java.util.List;

@RestController
@RequestMapping(path = "/compilations")
@RequiredArgsConstructor
public class CompilationController {
    private static final String PATH = "comp-id";
    private final CompilationService compilationService;

    @GetMapping("/{comp-id}")
    public ResponseEntity<CompilationDto> get(@PathVariable(PATH) @Positive long compId) {

        return ResponseEntity.ok()
                .body(compilationService.get(compId));
    }

    @GetMapping
    public ResponseEntity<List<CompilationDto>> getCompilations(
            @RequestParam(required = false, name = "pinned") Boolean pinned,
            @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(name = "size", defaultValue = "10") @PositiveOrZero int size) {

        CompilationSearchParam params = new CompilationSearchParam(pinned, from, size);

        return ResponseEntity.ok().body(compilationService.getCompilations(params));
    }
}