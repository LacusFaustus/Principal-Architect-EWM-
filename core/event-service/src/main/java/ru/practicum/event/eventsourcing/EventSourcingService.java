package ru.practicum.event.eventsourcing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.event.events.*;
import ru.practicum.eventsourcing.EventStore;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSourcingService {

    private final EventStore eventStore;

    public EventAggregate createEvent(CreateEventCommand command) {
        EventAggregate aggregate = new EventAggregate();
        aggregate.setEventStore(eventStore);
        aggregate.createEvent(command);
        return aggregate;
    }

    public EventAggregate publishEvent(String eventId) {
        EventAggregate aggregate = new EventAggregate();
        aggregate.setEventStore(eventStore);
        aggregate.publishEvent(new PublishEventCommand(eventId));
        return aggregate;
    }

    public EventAggregate cancelEvent(String eventId, String reason) {
        EventAggregate aggregate = new EventAggregate();
        aggregate.setEventStore(eventStore);
        aggregate.cancelEvent(new CancelEventCommand(eventId, reason));
        return aggregate;
    }

    public EventAggregate updateEvent(UpdateEventCommand command) {
        EventAggregate aggregate = new EventAggregate();
        aggregate.setEventStore(eventStore);
        aggregate.updateEvent(command);
        return aggregate;
    }
}