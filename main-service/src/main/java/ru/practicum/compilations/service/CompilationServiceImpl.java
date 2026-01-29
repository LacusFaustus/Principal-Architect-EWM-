package ru.practicum.compilations.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.compilations.dto.*;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.compilations.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;

    @Transactional
    @Override
    public CompilationDto add(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationRepository.save(compilationMapper.toModel(newCompilationDto));

        if (newCompilationDto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
            compilation.setEvents(events);
        }

        return compilationMapper.toCompilationDto(compilation);
    }

    @Transactional
    @Override
    public CompilationDto update(long id, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        compilationMapper.updateCompilationFromDto(updateCompilationRequest, compilation);

        if (updateCompilationRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateCompilationRequest.getEvents()));
            compilation.setEvents(events);
        }

        return compilationMapper.toCompilationDto(compilationRepository.save(compilation));
    }

    @Override
    public CompilationDto get(long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        return compilationMapper.toCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(CompilationSearchParam params) {
        Pageable pageable = PageRequest.of(
                params.getFrom() / params.getSize(),
                params.getSize(),
                Sort.by("id").ascending()
        );

        Page<Compilation> page;
        if (params.getPinned() != null) {
            page = compilationRepository.findByPinned(params.getPinned(), pageable);
        } else {
            page = compilationRepository.findAll(pageable);
        }

        return compilationMapper.toCompilationsDto(page.getContent());
    }

    @Transactional
    @Override
    public void delete(long id) {
        if (!compilationRepository.existsById(id)) {
            throw new NotFoundException("Compilation not found");
        }
        compilationRepository.deleteById(id);
    }
}
