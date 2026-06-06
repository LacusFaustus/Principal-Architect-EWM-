package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.model.Category;
import ru.practicum.dto.UserShortInfoDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.location.dto.Location;
import ru.practicum.location.model.LocationEntity;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", source = "userId")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "location", source = "newEventDto.location")
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto newEventDto, Category category, Long userId);

    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "annotation", source = "event.annotation")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "createdOn", source = "event.createdOn")
    @Mapping(target = "description", source = "event.description")
    @Mapping(target = "eventDate", source = "event.eventDate")
    @Mapping(target = "paid", source = "event.paid")
    @Mapping(target = "participantLimit", source = "event.participantLimit")
    @Mapping(target = "publishedOn", source = "event.publishedOn")
    @Mapping(target = "requestModeration", source = "event.requestModeration")
    @Mapping(target = "state", source = "event.state")
    @Mapping(target = "title", source = "event.title")
    EventFullDto toEventFullDto(Event event, UserShortInfoDto initiator, Double rating, Long confirmedRequests);

    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "annotation", source = "event.annotation")
    @Mapping(target = "eventDate", source = "event.eventDate")
    @Mapping(target = "paid", source = "event.paid")
    @Mapping(target = "title", source = "event.title")
    EventShortDto toEventShortDto(Event event, UserShortInfoDto initiator, Double rating, Long confirmedRequests);

    default LocationEntity map(Location location) {
        if (location == null) return null;
        return new LocationEntity(location.getLat(), location.getLon());
    }

    default Location map(LocationEntity location) {
        if (location == null) return null;
        return new Location(location.getLat(), location.getLon());
    }
}