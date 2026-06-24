package ru.practicum.eventsourcing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class EventAggregateRoot {
    private EventStore eventStore;
}