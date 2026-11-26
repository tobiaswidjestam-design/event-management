package se.lexicon.eventapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import se.lexicon.eventapp.db.ConnectionFactory;
import se.lexicon.eventapp.exception.DatabaseException;
import se.lexicon.eventapp.model.Participant;
import se.lexicon.eventapp.model.ParticipantType;

/**
 * Stores participants in the {@code participants} table.
 */
public class JdbcParticipantRepository implements ParticipantRepository {

    private static final String INSERT =
            "INSERT INTO participants (name, email, type, organization) VALUES (?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE participants SET name = ?, email = ?, type = ?, organization = ? WHERE id = ?";
    private static final String SELECT_ALL =
            "SELECT id, name, email, type, organization FROM participants ORDER BY name";
    private static final String SELECT_BY_ID =
            "SELECT id, name, email, type, organization FROM participants WHERE id = ?";
    private static final String SELECT_BY_EMAIL =
            "SELECT id, name, email, type, organization FROM participants WHERE email = ?";
    private static final String DELETE = "DELETE FROM participants WHERE id = ?";

    @Override
    public Participant save(Participant participant) {
        boolean isNew = participant.getId() == 0;

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     isNew ? INSERT : UPDATE, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, participant.getName());
            statement.setString(2, participant.getEmail());
            statement.setString(3, participant.getType().name());
            statement.setString(4, participant.getOrganization());

            if (isNew) {
                statement.executeUpdate();
                int id = readGeneratedId(statement);
                return new Participant(id, participant.getName(), participant.getEmail(),
                        participant.getType(), participant.getOrganization());
            }

            statement.setInt(5, participant.getId());
            statement.executeUpdate();
            return participant;
        } catch (SQLException e) {
            throw new DatabaseException("Could not save participant: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> findById(int id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {

            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read participant " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Participant> findByEmail(String email) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_EMAIL)) {

            statement.setString(1, email);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read participant by email: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Participant> findAll() {
        List<Participant> participants = new ArrayList<>();

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rows = statement.executeQuery()) {

            while (rows.next()) {
                participants.add(mapRow(rows));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read participants: " + e.getMessage(), e);
        }
        return participants;
    }

    @Override
    public boolean delete(int id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete participant " + id + ": " + e.getMessage(), e);
        }
    }

    private Participant mapRow(ResultSet rows) throws SQLException {
        return new Participant(
                rows.getInt("id"),
                rows.getString("name"),
                rows.getString("email"),
                ParticipantType.valueOf(rows.getString("type")),
                rows.getString("organization"));
    }

    private int readGeneratedId(Statement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("No generated key returned for the new participant");
        }
    }
}
