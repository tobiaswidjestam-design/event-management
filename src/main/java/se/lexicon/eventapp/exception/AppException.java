package se.lexicon.eventapp.exception;

/**
 * Base class for all exceptions that the application throws on purpose.
 * Catching this type in the user interface keeps the console menu alive on every expected error.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
