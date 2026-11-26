package se.lexicon.eventapp.exception;

/**
 * Thrown when an entity that the caller refers to does not exist.
 */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(message);
    }
}
