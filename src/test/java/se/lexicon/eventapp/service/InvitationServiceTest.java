package se.lexicon.eventapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.lexicon.eventapp.exception.DuplicateInvitationException;
import se.lexicon.eventapp.exception.OverbookedException;
import se.lexicon.eventapp.exception.ValidationException;
import se.lexicon.eventapp.model.Event;
import se.lexicon.eventapp.model.Invitation;
import se.lexicon.eventapp.model.InvitationStatus;
import se.lexicon.eventapp.model.Participant;
import se.lexicon.eventapp.model.ParticipantType;
import se.lexicon.eventapp.repository.EventRepository;
import se.lexicon.eventapp.repository.InvitationRepository;
import se.lexicon.eventapp.repository.ParticipantRepository;

/**
 * Tests the rules that span several objects. The repositories are replaced by in-memory
 * implementations, which is possible because the service depends on the interfaces only.
 */
class InvitationServiceTest {

    private static final LocalDateTime START = LocalDateTime.now().plusDays(7);

    private InMemoryParticipantRepository participants;
    private InMemoryEventRepository events;
    private InMemoryInvitationRepository invitations;
    private InvitationService invitationService;

    private Event event;
    private Participant anna;
    private Participant bella;
    private Participant carl;

    @BeforeEach
    void setUp() {
        participants = new InMemoryParticipantRepository();
        events = new InMemoryEventRepository();
        invitations = new InMemoryInvitationRepository();

        invitationService = new InvitationService(invitations, events, participants);

        event = events.save(new Event("Workshop", "", START, START.plusHours(2), "Room A", 2));
        anna = participants.save(new Participant("Anna", "anna@example.com", ParticipantType.INDIVIDUAL, null));
        bella = participants.save(new Participant("Bella", "bella@example.com", ParticipantType.INDIVIDUAL, null));
        carl = participants.save(new Participant("Carl", "carl@example.com", ParticipantType.INDIVIDUAL, null));
    }

    @Test
    void inviteCreatesPendingInvitation() {
        Invitation invitation = invitationService.invite(event.getId(), anna.getId());

        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertEquals(1, invitationService.invitationsFor(event.getId()).size());
    }

    @Test
    void sameParticipantCannotBeInvitedTwice() {
        invitationService.invite(event.getId(), anna.getId());

        assertThrows(DuplicateInvitationException.class,
                () -> invitationService.invite(event.getId(), anna.getId()));
    }

    @Test
    void cannotInviteToAnEventThatHasEnded() {
        Event past = events.save(new Event("Old", "", START.minusDays(10), START.minusDays(9), "Room B", 5));

        assertThrows(ValidationException.class, () -> invitationService.invite(past.getId(), anna.getId()));
    }

    @Test
    void acceptsInvitationsWhileSeatsAreFree() {
        invitationService.invite(event.getId(), anna.getId());
        invitationService.invite(event.getId(), bella.getId());

        invitationService.accept(1);
        invitationService.accept(2);

        assertEquals(2, invitationService.attendeesFor(event.getId()).size());
    }

    @Test
    void refusesToOverbookTheEvent() {
        invitationService.invite(event.getId(), anna.getId());
        invitationService.invite(event.getId(), bella.getId());
        invitationService.invite(event.getId(), carl.getId());

        invitationService.accept(1);
        invitationService.accept(2);

        assertThrows(OverbookedException.class, () -> invitationService.accept(3));
    }

    @Test
    void declinedInvitationDoesNotTakeASeat() {
        invitationService.invite(event.getId(), anna.getId());
        invitationService.invite(event.getId(), bella.getId());
        invitationService.invite(event.getId(), carl.getId());

        invitationService.decline(1);
        invitationService.accept(2);
        invitationService.accept(3);

        assertEquals(2, invitationService.attendeesFor(event.getId()).size());
    }

    @Test
    void attendanceListContainsOnlyAcceptedParticipantsSortedByName() {
        invitationService.invite(event.getId(), bella.getId());
        invitationService.invite(event.getId(), anna.getId());
        invitationService.invite(event.getId(), carl.getId());

        invitationService.accept(1);
        invitationService.accept(2);

        List<Participant> attendees = invitationService.attendeesFor(event.getId());
        assertEquals(List.of("Anna", "Bella"),
                attendees.stream().map(Participant::getName).toList());
    }

    @Test
    void eventsForParticipantIsSortedByStart() {
        Event later = events.save(new Event("Later", "", START.plusDays(5), START.plusDays(5).plusHours(1),
                "Room C", 10));
        invitationService.invite(later.getId(), anna.getId());
        invitationService.invite(event.getId(), anna.getId());

        List<Event> result = invitationService.eventsForParticipant(anna.getId());

        assertEquals(List.of("Workshop", "Later"), result.stream().map(Event::getTitle).toList());
    }

    // ---------------------------------------------------------------- in-memory repositories

    static class InMemoryParticipantRepository implements ParticipantRepository {

        private final Map<Integer, Participant> store = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public Participant save(Participant participant) {
            Participant stored = new Participant(
                    participant.getId() == 0 ? nextId++ : participant.getId(),
                    participant.getName(), participant.getEmail(), participant.getType(),
                    participant.getOrganization());
            store.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Optional<Participant> findById(int id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Participant> findByEmail(String email) {
            return store.values().stream().filter(p -> p.getEmail().equals(email)).findFirst();
        }

        @Override
        public List<Participant> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public boolean delete(int id) {
            return store.remove(id) != null;
        }
    }

    static class InMemoryEventRepository implements EventRepository {

        private final Map<Integer, Event> store = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public Event save(Event event) {
            Event stored = new Event(
                    event.getId() == 0 ? nextId++ : event.getId(),
                    event.getTitle(), event.getDescription(), event.getStart(), event.getEnd(),
                    event.getLocation(), event.getCapacity());
            store.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Optional<Event> findById(int id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Event> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Event> findUpcoming(LocalDateTime from) {
            return store.values().stream().filter(event -> event.getEnd().isAfter(from)).toList();
        }

        @Override
        public boolean delete(int id) {
            return store.remove(id) != null;
        }
    }

    static class InMemoryInvitationRepository implements InvitationRepository {

        private final Map<Integer, Invitation> store = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public Invitation save(Invitation invitation) {
            Invitation stored = new Invitation(
                    invitation.getId() == 0 ? nextId++ : invitation.getId(),
                    invitation.getEvent(), invitation.getParticipant(), invitation.getStatus(),
                    invitation.getInvitedAt());
            store.put(stored.getId(), stored);
            return stored;
        }

        @Override
        public Optional<Invitation> findById(int id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Invitation> findByEventAndParticipant(int eventId, int participantId) {
            return store.values().stream()
                    .filter(i -> i.getEvent().getId() == eventId)
                    .filter(i -> i.getParticipant().getId() == participantId)
                    .findFirst();
        }

        @Override
        public List<Invitation> findByEvent(int eventId) {
            return store.values().stream().filter(i -> i.getEvent().getId() == eventId).toList();
        }

        @Override
        public List<Invitation> findByParticipant(int participantId) {
            return store.values().stream().filter(i -> i.getParticipant().getId() == participantId).toList();
        }

        @Override
        public List<Invitation> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public long countAccepted(int eventId) {
            return store.values().stream()
                    .filter(i -> i.getEvent().getId() == eventId)
                    .filter(Invitation::isAccepted)
                    .count();
        }

        @Override
        public void updateStatus(int invitationId, InvitationStatus status) {
            Invitation current = store.get(invitationId);
            if (current != null) {
                store.put(invitationId, new Invitation(invitationId, current.getEvent(),
                        current.getParticipant(), status, current.getInvitedAt()));
            }
        }

        @Override
        public boolean delete(int id) {
            return store.remove(id) != null;
        }
    }
}
