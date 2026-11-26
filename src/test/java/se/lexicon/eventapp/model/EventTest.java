package se.lexicon.eventapp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import se.lexicon.eventapp.exception.ValidationException;

class EventTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 18, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 12, 1, 20, 0);

    @Test
    void createsValidEvent() {
        Event event = new Event("Java meetup", "Monthly meetup", START, END, "Room A", 20);

        assertEquals("Java meetup", event.getTitle());
        assertEquals(20, event.getCapacity());
        assertEquals(START, event.getStart());
    }

    @Test
    void rejectsEventThatEndsBeforeItStarts() {
        assertThrows(ValidationException.class,
                () -> new Event("Broken", "", END, START, "Room A", 10));
    }

    @Test
    void rejectsEventWithZeroCapacity() {
        assertThrows(ValidationException.class,
                () -> new Event("Empty", "", START, END, "Room A", 0));
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(ValidationException.class,
                () -> new Event("  ", "", START, END, "Room A", 10));
    }

    @Test
    void knowsWhenThereIsAFreeSeat() {
        Event event = new Event("Workshop", "", START, END, "Room B", 2);

        assertTrue(event.hasFreeSeat(1));
        assertFalse(event.hasFreeSeat(2));
        assertEquals(0, event.availableSeats(5));
        assertEquals(1, event.availableSeats(1));
    }

    @Test
    void knowsWhenItHasEnded() {
        Event past = new Event("Old event", "", START.minusDays(10), END.minusDays(10), "Room C", 5);
        Event future = new Event("New event", "", START.plusDays(10), END.plusDays(10), "Room C", 5);

        assertTrue(past.hasEnded());
        assertFalse(future.hasEnded());
    }
}
