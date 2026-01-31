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


@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toModel(NewCompilationDto dto);

    @Mapping(target = "events", source = "events", qualifiedByName = "eventsToEventShortDtos")
    CompilationDto toCompilationDto(Compilation compilation);

    List<CompilationDto> toCompilationDtoList(List<Compilation> compilations);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateCompilationFromDto(UpdateCompilationRequest dto, @MappingTarget Compilation compilation);

    @Named("eventsToEventShortDtos")
    default List<EventShortDto> eventsToEventShortDtos(Set<Event> events) {
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .map(this::eventToEventShortDto)
                .collect(Collectors.toList());
    }

    default EventShortDto eventToEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());

        if (event.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto(event.getCategory().getId(), event.getCategory().getName());
            dto.setCategory(categoryDto);
        }


        dto.setConfirmedRequests(0L);
        dto.setEventDate(event.getEventDate());

        if (event.getInitiator() != null) {
            UserShortDto initiatorDto = new UserShortDto(event.getInitiator().getName(), event.getInitiator().getEmail());
            dto.setInitiator(initiatorDto);
        }

        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());

        dto.setViews(0L);

        return dto;
    }
}
