package se.lexicon.eventapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import se.lexicon.eventapp.db.ConnectionFactory;
import se.lexicon.eventapp.exception.DatabaseException;
import se.lexicon.eventapp.model.Event;

/**
 * Stores events in the {@code events} table.
 */
public class JdbcEventRepository implements EventRepository {

    private static final String INSERT = "INSERT INTO events "
            + "(title, description, start_time, end_time, location, capacity) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE events SET title = ?, description = ?, start_time = ?, "
            + "end_time = ?, location = ?, capacity = ? WHERE id = ?";
    private static final String COLUMNS =
            "SELECT id, title, description, start_time, end_time, location, capacity FROM events";
    private static final String SELECT_ALL = COLUMNS + " ORDER BY start_time";
    private static final String SELECT_BY_ID = COLUMNS + " WHERE id = ?";
    private static final String SELECT_UPCOMING = COLUMNS + " WHERE end_time > ? ORDER BY start_time";
    private static final String DELETE = "DELETE FROM events WHERE id = ?";

    @Override
    public Event save(Event event) {
        boolean isNew = event.getId() == 0;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     isNew ? INSERT : UPDATE, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, event.getTitle());
            statement.setString(2, event.getDescription());
            statement.setTimestamp(3, Timestamp.valueOf(event.getStart()));
            statement.setTimestamp(4, Timestamp.valueOf(event.getEnd()));
            statement.setString(5, event.getLocation());
            statement.setInt(6, event.getCapacity());

            if (isNew) {
                statement.executeUpdate();
                int id = readGeneratedId(statement);
                return new Event(id, event.getTitle(), event.getDescription(), event.getStart(),
                        event.getEnd(), event.getLocation(), event.getCapacity());
            }

            statement.setInt(7, event.getId());
            statement.executeUpdate();
            return event;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save event: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Event> findById(int id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read event " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Event> findAll() {
        return readList(SELECT_ALL, null);
    }

    @Override
    public List<Event> findUpcoming(LocalDateTime from) {
        return readList(SELECT_UPCOMING, from);
    }

    @Override
    public boolean delete(int id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete event " + id + ": " + e.getMessage(), e);
        }
    }

    private List<Event> readList(String sql, LocalDateTime parameter) {
        List<Event> events = new ArrayList<>();

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setTimestamp(1, Timestamp.valueOf(parameter));
            }

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    events.add(mapRow(rows));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read events: " + e.getMessage(), e);
        }
        return events;
    }

    private Event mapRow(ResultSet rows) throws SQLException {
        return new Event(
                rows.getInt("id"),
                rows.getString("title"),
                rows.getString("description"),
                rows.getTimestamp("start_time").toLocalDateTime(),
                rows.getTimestamp("end_time").toLocalDateTime(),
                rows.getString("location"),
                rows.getInt("capacity"));
    }

    private int readGeneratedId(Statement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("No generated key returned for the new event");
        }
    }
}
