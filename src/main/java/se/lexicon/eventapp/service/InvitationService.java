package se.lexicon.eventapp.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import se.lexicon.eventapp.exception.DuplicateInvitationException;
import se.lexicon.eventapp.exception.NotFoundException;
import se.lexicon.eventapp.exception.OverbookedException;
import se.lexicon.eventapp.exception.ValidationException;
import se.lexicon.eventapp.model.Event;
import se.lexicon.eventapp.model.Invitation;
import se.lexicon.eventapp.model.InvitationStatus;
import se.lexicon.eventapp.model.Participant;
import se.lexicon.eventapp.repository.EventRepository;
import se.lexicon.eventapp.repository.InvitationRepository;
import se.lexicon.eventapp.repository.ParticipantRepository;

/**
 * The rules that involve more than one object: inviting, answering an invitation,
 * preventing duplicate invitations and preventing overbooking.
 *
 * The methods that change data are synchronized. The application is a single-user console,
 * so this is enough to stop two answers from being counted at the same time; the unique
 * constraint on (event_id, participant_id) protects the duplicate rule in the database as well.
 */
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    public InvitationService(InvitationRepository invitationRepository,
                             EventRepository eventRepository,
                             ParticipantRepository participantRepository) {
        this.invitationRepository = invitationRepository;
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
    }

    /**
     * Invites a participant to an event. Refuses duplicate invitations and events that already ended.
     */
    public synchronized Invitation invite(int eventId, int participantId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("No event with id " + eventId));
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NotFoundException("No participant with id " + participantId));

        if (event.hasEnded()) {
            throw new ValidationException("Event \"" + event.getTitle() + "\" has already ended");
        }

        invitationRepository.findByEventAndParticipant(eventId, participantId).ifPresent(existing -> {
            throw new DuplicateInvitationException(
                    participant.getName() + " is already invited to \"" + event.getTitle() + "\"");
        });

        return invitationRepository.save(new Invitation(event, participant));
    }

    /**
     * Accepts an invitation. The number of accepted invitations is read from the database
     * on every call, so the capacity rule is checked against the current state.
     */
    public synchronized void accept(int invitationId) {
        Invitation invitation = getInvitation(invitationId);
        Event event = invitation.getEvent();

        long accepted = invitationRepository.countAccepted(event.getId());
        if (!invitation.isAccepted() && !event.hasFreeSeat((int) accepted)) {
            throw new OverbookedException(String.format(
                    "\"%s\" is full: %d of %d seats are taken",
                    event.getTitle(), accepted, event.getCapacity()));
        }

        invitation.accept();
        invitationRepository.updateStatus(invitation.getId(), InvitationStatus.ACCEPTED);
    }

    /**
     * Declines a pending invitation. No capacity check is needed here, because declining can
     * only free a seat, never take one.
     */
    public synchronized void decline(int invitationId) {
        Invitation invitation = getInvitation(invitationId);
        invitation.decline();
        invitationRepository.updateStatus(invitation.getId(), InvitationStatus.DECLINED);
    }

    /**
     * All invitations for an event, including their status.
     */
    public List<Invitation> invitationsFor(int eventId) {
        return invitationRepository.findByEvent(eventId).stream()
                .sorted(Comparator.comparing(invitation -> invitation.getParticipant().getName()))
                .collect(Collectors.toList());
    }

    /**
     * The attendance list: participants who accepted, sorted by name.
     */
    public List<Participant> attendeesFor(int eventId) {
        return invitationRepository.findByEvent(eventId).stream()
                .filter(Invitation::isAccepted)
                .map(Invitation::getParticipant)
                .sorted(Comparator.comparing(Participant::getName))
                .collect(Collectors.toList());
    }

    /**
     * The events a participant is invited to, earliest first, without duplicates.
     */
    public List<Event> eventsForParticipant(int participantId) {
        return invitationRepository.findByParticipant(participantId).stream()
                .map(Invitation::getEvent)
                .collect(Collectors.toMap(Event::getId, event -> event, (first, second) -> first,
                        LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    /**
     * Loads one invitation, or throws NotFoundException when the id does not exist.
     */
    public Invitation getInvitation(int invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("No invitation with id " + invitationId));
    }
}
