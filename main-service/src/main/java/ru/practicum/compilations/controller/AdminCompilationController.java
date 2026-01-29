package ru.practicum.compilations.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    CompilationDto add(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        CompilationDto compilationDto = compilationService.add(newCompilationDto);

        return compilationDto;
    }

    @PatchMapping("/{comp-id}")
    CompilationDto update(@PathVariable(PATH) @Positive long compId,
                          @RequestBody @Valid UpdateCompilationRequest updateCompilationDto) {
        CompilationDto compilationDto = compilationService.update(compId, updateCompilationDto);

        return compilationDto;
    }

    @DeleteMapping("/{comp-id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable(PATH) @Positive long compId) {
        compilationService.delete(compId);
    }
}