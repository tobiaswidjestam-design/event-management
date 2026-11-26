package se.lexicon.eventapp.repository;

import java.util.List;
import java.util.Optional;
import se.lexicon.eventapp.model.Invitation;
import se.lexicon.eventapp.model.InvitationStatus;

public interface InvitationRepository extends Repository<Invitation> {

    Optional<Invitation> findByEventAndParticipant(int eventId, int participantId);

    List<Invitation> findByEvent(int eventId);

    List<Invitation> findByParticipant(int participantId);

    /**
     * Counted in the database, so the number is always current when the capacity rule is checked.
     */
    long countAccepted(int eventId);

    void updateStatus(int invitationId, InvitationStatus status);
}
