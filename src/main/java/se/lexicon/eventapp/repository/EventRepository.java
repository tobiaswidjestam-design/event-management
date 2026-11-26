package se.lexicon.eventapp.repository;

import java.time.LocalDateTime;
import java.util.List;
import se.lexicon.eventapp.model.Event;

public interface EventRepository extends Repository<Event> {

    /**
     * @param from events that end after this point in time
     * @return the events sorted by start time
     */
    List<Event> findUpcoming(LocalDateTime from);
}
