package se.lexicon.eventapp.exception;

/**
 * Thrown when input or a state change violates a domain rule, for example an empty name,
 * an event that ends before it starts, or a non-pending invitation being accepted.
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message);
    }
}
