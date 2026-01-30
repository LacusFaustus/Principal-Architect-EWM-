package ru.practicum.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.request.dto.NewRequestDto;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.Request;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    @Mapping(target = "id", ignore = true)
    Request mapToRequest(NewRequestDto newRequestDto);

    @Mapping(target = "requester", source = "requester.id")
    @Mapping(target = "event", source = "event.id")
    ParticipationRequestDto mapToRequestDto(Request request);
}
