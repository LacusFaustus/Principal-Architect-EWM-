package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.model.Category;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.location.dto.Location;
import ru.practicum.location.model.LocationEntity;
import ru.practicum.user.model.User;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "user")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", expression = "java(ru.practicum.event.model.EventState.PENDING)")
    @Mapping(target = "location", source = "newEventDto.location")
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto newEventDto, Category category, User user);

    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "views", source = "views", defaultValue = "0L")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests", defaultValue = "0L")
    EventFullDto toEventFullDto(Event event, Long views, Long confirmedRequests);

    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    @Mapping(target = "views", source = "views", defaultValue = "0L")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests", defaultValue = "0L")
    EventShortDto toEventShortDto(Event event, Long views, Long confirmedRequests);

    default LocationEntity map(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationEntity(location.getLat(), location.getLon());
    }

    default Location map(LocationEntity location) {
        if (location == null) {
            return null;
        }
        return new Location(location.getLat(), location.getLon());
    }
}