package ru.practicum.compilations.service;

import ru.practicum.compilations.dto.CompilationDto;
import ru.practicum.compilations.dto.CompilationSearchParam;
import ru.practicum.compilations.dto.NewCompilationDto;
import ru.practicum.compilations.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto add(NewCompilationDto newCompilationDto);

    CompilationDto update(long compId, UpdateCompilationRequest updateCompilationDto);

    CompilationDto get(long compId);

    List<CompilationDto> getCompilations(CompilationSearchParam params);

    void delete(long compId);
}