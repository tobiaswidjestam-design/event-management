package se.lexicon.eventapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import se.lexicon.eventapp.exception.ValidationException;

/**
 * An event organized by the community center.
 * The event owns its own rules: a valid time range, a positive capacity and whether it has ended.
 */
public class Event {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private String location;
    private int capacity;

    /**
     * Creates a new event and validates it: the title, the location and the capacity, and that
     * the end time is after the start time.
     */
    public Event(String title, String description, LocalDateTime start, LocalDateTime end,
                 String location, int capacity) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Event title is required");
        }
        if (start == null || end == null) {
            throw new ValidationException("Event start and end are required");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("The event must end after it starts");
        }
        if (location == null || location.isBlank()) {
            throw new ValidationException("Event location is required");
        }
        if (capacity <= 0) {
            throw new ValidationException("Capacity must be greater than zero");
        }

        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.start = start;
        this.end = end;
        this.location = location.trim();
        this.capacity = capacity;
    }

    /**
     * Used by the repository when an event is read from the database.
     */
    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end,
                 String location, int capacity) {
        this(title, description, start, end, location, capacity);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public String getLocation() {
        return location;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * @param accepted how many invitations are currently accepted for this event
     * @return true if there is at least one seat left
     */
    public boolean hasFreeSeat(int accepted) {
        return accepted < capacity;
    }

    /**
     * Seats left on the event right now, never below zero.
     */
    public int availableSeats(int accepted) {
        return Math.max(0, capacity - accepted);
    }

    /**
     * True when the end time has passed. An event that has ended cannot get new invitations.
     */
    public boolean hasEnded() {
        return end.isBefore(LocalDateTime.now());
    }

    @Override
    public String toString() {
        String when = String.format("%s - %s", FORMATTER.format(start), FORMATTER.format(end));
        return String.format("#%d %s at %s (%s), capacity %d", id, title, location, when, capacity);
    }
}
