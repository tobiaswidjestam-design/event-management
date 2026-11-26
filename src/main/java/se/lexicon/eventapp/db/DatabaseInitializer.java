package se.lexicon.eventapp.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import se.lexicon.eventapp.exception.DatabaseException;

/**
 * Creates the tables on startup by running {@code schema.sql} from the classpath.
 * The statements use CREATE TABLE IF NOT EXISTS, so starting the application twice is harmless.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String script = readSchema();

        try (Connection connection = ConnectionFactory.getConnection();
             Statement statement = connection.createStatement()) {

            for (String sqlStatement : script.split(";")) {
                String sql = stripComments(sqlStatement).trim();
                if (!sql.isEmpty()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not create the database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Removes full-line {@code --} comments so that statements can be executed one by one.
     */
    private static String stripComments(String sql) {
        StringBuilder builder = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private static String readSchema() {
        try (InputStream input = DatabaseInitializer.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("schema.sql was not found on the classpath");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseException("Could not read schema.sql", e);
        }
    }
}
