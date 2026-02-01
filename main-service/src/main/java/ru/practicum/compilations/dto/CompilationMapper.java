package ru.practicum.compilations.dto;

import org.mapstruct.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.compilations.model.Compilation;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.user.dto.UserShortDto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto dto);

    @Mapping(target = "events", source = "eventShortDtos")
    CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> eventShortDtos);

    List<CompilationDto> toCompilationDtoList(List<Compilation> compilations);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateCompilationFromDto(UpdateCompilationRequest dto, @MappingTarget Compilation compilation);
}
