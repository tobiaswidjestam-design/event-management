package se.lexicon.eventapp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import se.lexicon.eventapp.exception.ValidationException;

class InvitationTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 18, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 12, 1, 20, 0);

    private final Event event = new Event(1, "Meetup", "", START, END, "Room A", 2);
    private final Participant participant =
            new Participant(1, "Anna", "anna@example.com", ParticipantType.INDIVIDUAL, null);

    @Test
    void newInvitationIsPending() {
        Invitation invitation = new Invitation(event, participant);

        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertTrue(invitation.isPending());
    }

    @Test
    void canBeAccepted() {
        Invitation invitation = new Invitation(event, participant);
        invitation.accept();

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        assertTrue(invitation.isAccepted());
    }

    @Test
    void canBeDeclined() {
        Invitation invitation = new Invitation(event, participant);
        invitation.decline();

        assertEquals(InvitationStatus.DECLINED, invitation.getStatus());
    }

    @Test
    void cannotBeAcceptedTwice() {
        Invitation invitation = new Invitation(event, participant);
        invitation.accept();

        assertThrows(ValidationException.class, invitation::accept);
    }

    @Test
    void cannotBeDeclinedAfterBeingAccepted() {
        Invitation invitation = new Invitation(event, participant);
        invitation.accept();

        assertThrows(ValidationException.class, invitation::decline);
    }

    @Test
    void requiresEventAndParticipant() {
        assertThrows(ValidationException.class, () -> new Invitation(null, participant));
        assertThrows(ValidationException.class, () -> new Invitation(event, null));
    }
}
