package se.lexicon.eventapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import se.lexicon.eventapp.exception.ValidationException;

/**
 * Links a participant to an event and keeps track of the answer.
 * The status can only be changed through {@link #accept()} and {@link #decline()},
 * which is what enforces the rule that an invitation is answered at most once.
 */
public class Invitation {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private Event event;
    private Participant participant;
    private InvitationStatus status;
    private LocalDateTime invitedAt;

    public Invitation(Event event, Participant participant) {
        if (event == null || participant == null) {
            throw new ValidationException("An invitation needs both an event and a participant");
        }
        this.event = event;
        this.participant = participant;
        this.status = InvitationStatus.PENDING;
        this.invitedAt = LocalDateTime.now();
    }

    /**
     * Used by the repository when an invitation is read from the database.
     */
    public Invitation(int id, Event event, Participant participant, InvitationStatus status,
                      LocalDateTime invitedAt) {
        this(event, participant);
        this.id = id;
        this.status = status == null ? InvitationStatus.PENDING : status;
        this.invitedAt = invitedAt == null ? LocalDateTime.now() : invitedAt;
    }

    public int getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Participant getParticipant() {
        return participant;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public void accept() {
        requirePending("accepted");
        this.status = InvitationStatus.ACCEPTED;
    }

    public void decline() {
        requirePending("declined");
        this.status = InvitationStatus.DECLINED;
    }

    private void requirePending(String action) {
        if (status != InvitationStatus.PENDING) {
            throw new ValidationException(
                    "Invitation " + id + " is already " + status + " and cannot be " + action);
        }
    }

    @Override
    public String toString() {
        return String.format("#%d %s -> %s [%s] invited %s",
                id, participant.getName(), event.getTitle(), status, FORMATTER.format(invitedAt));
    }
}
