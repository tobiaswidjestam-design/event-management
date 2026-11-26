package se.lexicon.eventapp.exception;

/**
 * Wraps {@link java.sql.SQLException} so that the rest of the application never has to handle
 * checked SQL exceptions. The original exception is kept as the cause for debugging.
 */
public class DatabaseException extends AppException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
