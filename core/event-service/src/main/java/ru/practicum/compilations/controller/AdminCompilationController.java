package ru.practicum.compilations.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;
import ru.practicum.compilations.service.CompilationService;

@RestController
@RequestMapping(path = "/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {
    private static final String PATH = "comp-id";
    private final CompilationService compilationService;

    @PostMapping()
    public ResponseEntity<CompilationDto> add(
            @RequestBody @Valid NewCompilationDto newCompilationDto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compilationService.add(newCompilationDto));
    }

    @PatchMapping("/{comp-id}")
    public ResponseEntity<CompilationDto> update(
            @PathVariable(PATH) @Positive long compId,
            @RequestBody @Valid UpdateCompilationRequest updateCompilationDto) {

        return ResponseEntity.ok()
                .body(compilationService.update(compId, updateCompilationDto));
    }

    @DeleteMapping("/{comp-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(
            @PathVariable(PATH) @Positive long compId) {

        compilationService.delete(compId);

        return ResponseEntity.noContent()
                .build();
    }
}