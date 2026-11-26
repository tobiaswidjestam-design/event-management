package se.lexicon.eventapp.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import se.lexicon.eventapp.exception.DatabaseException;

/**
 * Creates JDBC connections from the settings in {@code db.properties}.
 * Every repository opens and closes its own connection, which keeps the code simple
 * and means a closed result set can never leak between two calls.
 */
public final class ConnectionFactory {

    private static final Properties SETTINGS = loadSettings();

    private ConnectionFactory() {
    }

    public static Connection getConnection() {
        String url = SETTINGS.getProperty("db.url");
        String user = SETTINGS.getProperty("db.user");
        String password = SETTINGS.getProperty("db.password");

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Could not connect to the database (" + url + "): " + e.getMessage(), e);
        }
    }

    private static Properties loadSettings() {
        Properties properties = new Properties();
        try (InputStream input = ConnectionFactory.class.getResourceAsStream("/db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties was not found on the classpath");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read db.properties", e);
        }
        return properties;
    }
}
