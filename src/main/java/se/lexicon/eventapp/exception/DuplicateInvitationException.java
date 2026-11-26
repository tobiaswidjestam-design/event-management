package se.lexicon.eventapp.exception;

/**
 * Thrown when a participant is invited to an event they are already invited to.
 */
public class DuplicateInvitationException extends AppException {

    public DuplicateInvitationException(String message) {
        super(message);
    }
}
