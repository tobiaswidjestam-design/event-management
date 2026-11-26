package se.lexicon.eventapp.exception;

/**
 * Thrown when accepting an invitation would exceed the capacity of the event.
 */
public class OverbookedException extends AppException {

    public OverbookedException(String message) {
        super(message);
    }
}
