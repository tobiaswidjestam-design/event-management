package se.lexicon.eventapp.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import se.lexicon.eventapp.db.ConnectionFactory;
import se.lexicon.eventapp.exception.DatabaseException;
import se.lexicon.eventapp.model.Event;
import se.lexicon.eventapp.model.Invitation;
import se.lexicon.eventapp.model.InvitationStatus;
import se.lexicon.eventapp.model.Participant;
import se.lexicon.eventapp.model.ParticipantType;

/**
 * Stores invitations in the {@code invitations} table.
 * Reads use a join so that every invitation comes back with its event and participant already loaded.
 */
public class JdbcInvitationRepository implements InvitationRepository {

    private static final String SELECT = "SELECT i.id AS invitation_id, i.status, i.invited_at, "
            + "e.id AS event_id, e.title, e.description, e.start_time, e.end_time, e.location, e.capacity, "
            + "p.id AS participant_id, p.name, p.email, p.type, p.organization "
            + "FROM invitations i "
            + "JOIN events e ON e.id = i.event_id "
            + "JOIN participants p ON p.id = i.participant_id ";

    private static final String INSERT =
            "INSERT INTO invitations (event_id, participant_id, status) VALUES (?, ?, ?)";
    private static final String UPDATE_STATUS = "UPDATE invitations SET status = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM invitations WHERE id = ?";
    private static final String COUNT_ACCEPTED =
            "SELECT COUNT(*) FROM invitations WHERE event_id = ? AND status = 'ACCEPTED'";
    private static final int DUPLICATE_KEY_ERROR = 1062;

    @Override
    public Invitation save(Invitation invitation) {
        if (invitation.getId() != 0) {
            updateStatus(invitation.getId(), invitation.getStatus());
            return invitation;
        }

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, invitation.getEvent().getId());
            statement.setInt(2, invitation.getParticipant().getId());
            statement.setString(3, invitation.getStatus().name());
            statement.executeUpdate();

            int id = readGeneratedId(statement);
            return new Invitation(id, invitation.getEvent(), invitation.getParticipant(),
                    invitation.getStatus(), invitation.getInvitedAt());
        } catch (SQLException e) {
            if (e.getErrorCode() == DUPLICATE_KEY_ERROR) {
                throw new DatabaseException("That participant is already invited to this event", e);
            }
            throw new DatabaseException("Could not save invitation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Invitation> findById(int id) {
        return readOne(SELECT + "WHERE i.id = ?", id);
    }

    @Override
    public Optional<Invitation> findByEventAndParticipant(int eventId, int participantId) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT + "WHERE i.event_id = ? AND i.participant_id = ?")) {

            statement.setInt(1, eventId);
            statement.setInt(2, participantId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read invitation: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Invitation> findByEvent(int eventId) {
        return readList(SELECT + "WHERE i.event_id = ? ORDER BY p.name", eventId);
    }

    @Override
    public List<Invitation> findByParticipant(int participantId) {
        return readList(SELECT + "WHERE i.participant_id = ? ORDER BY e.start_time", participantId);
    }

    @Override
    public List<Invitation> findAll() {
        return readList(SELECT + "ORDER BY e.start_time, p.name", null);
    }

    @Override
    public long countAccepted(int eventId) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_ACCEPTED)) {

            statement.setInt(1, eventId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not count accepted invitations: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(int invitationId, InvitationStatus status) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {

            statement.setString(1, status.name());
            statement.setInt(2, invitationId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not update invitation status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete invitation " + id + ": " + e.getMessage(), e);
        }
    }

    private Optional<Invitation> readOne(String sql, int id) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapRow(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read invitation " + id + ": " + e.getMessage(), e);
        }
    }

    private List<Invitation> readList(String sql, Integer parameter) {
        List<Invitation> invitations = new ArrayList<>();

        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (parameter != null) {
                statement.setInt(1, parameter);
            }

            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    invitations.add(mapRow(rows));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read invitations: " + e.getMessage(), e);
        }
        return invitations;
    }

    private Invitation mapRow(ResultSet rows) throws SQLException {
        Event event = new Event(
                rows.getInt("event_id"),
                rows.getString("title"),
                rows.getString("description"),
                rows.getTimestamp("start_time").toLocalDateTime(),
                rows.getTimestamp("end_time").toLocalDateTime(),
                rows.getString("location"),
                rows.getInt("capacity"));

        Participant participant = new Participant(
                rows.getInt("participant_id"),
                rows.getString("name"),
                rows.getString("email"),
                ParticipantType.valueOf(rows.getString("type")),
                rows.getString("organization"));

        Timestamp invitedAt = rows.getTimestamp("invited_at");

        return new Invitation(
                rows.getInt("invitation_id"),
                event,
                participant,
                InvitationStatus.valueOf(rows.getString("status")),
                invitedAt == null ? null : invitedAt.toLocalDateTime());
    }

    private int readGeneratedId(Statement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("No generated key returned for the new invitation");
        }
    }
}
