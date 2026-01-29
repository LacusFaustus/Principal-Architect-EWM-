package ru.practicum.compilations.dto;

import org.mapstruct.*;
import ru.practicum.compilations.model.Compilation;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {
    @Mapping(target = "events", ignore = true)
    Compilation toModel(NewCompilationDto newCompilationDto);

    CompilationDto toCompilationDto(Compilation compilation);

    List<CompilationDto> toCompilationsDto(List<Compilation> compilations);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "events", ignore = true)
    void updateCompilationFromDto(
            UpdateCompilationRequest dto,
            @MappingTarget Compilation compilation
    );
}
