package ru.practicum.compilations.dto;

import org.mapstruct.*;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto dto);

    @Mapping(target = "events", source = "eventShortDtos")
    CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> eventShortDtos);
}