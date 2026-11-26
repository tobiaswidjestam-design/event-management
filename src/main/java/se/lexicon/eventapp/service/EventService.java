package se.lexicon.eventapp.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import se.lexicon.eventapp.exception.NotFoundException;
import se.lexicon.eventapp.model.Event;
import se.lexicon.eventapp.repository.EventRepository;
import se.lexicon.eventapp.repository.InvitationRepository;

/**
 * Use cases for events: create, list and check how many seats are left.
 */
public class EventService {

    private final EventRepository eventRepository;
    private final InvitationRepository invitationRepository;

    public EventService(EventRepository eventRepository, InvitationRepository invitationRepository) {
        this.eventRepository = eventRepository;
        this.invitationRepository = invitationRepository;
    }

    /**
     * Creates an event and stores it. The Event constructor does the validation, so an event
     * that breaks a rule never reaches the database.
     */
    public Event createEvent(String title, String description, LocalDateTime start, LocalDateTime end,
                             String location, int capacity) {
        Event event = new Event(title, description, start, end, location, capacity);
        return eventRepository.save(event);
    }

    /**
     * All events that are stored, in the order the repository returns them.
     */
    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    /**
     * Events that are still running or have not started yet, earliest first.
     */
    public List<Event> findUpcoming() {
        return eventRepository.findUpcoming(LocalDateTime.now()).stream()
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    /**
     * Loads one event, or throws NotFoundException when the id does not exist.
     */
    public Event findById(int id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No event with id " + id));
    }

    /**
     * Seats left, calculated from the invitations that are accepted right now.
     */
    public int availableSeats(int eventId) {
        Event event = findById(eventId);
        long accepted = invitationRepository.countAccepted(eventId);
        return event.availableSeats((int) accepted);
    }
}
