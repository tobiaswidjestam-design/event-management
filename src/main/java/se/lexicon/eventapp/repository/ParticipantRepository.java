package se.lexicon.eventapp.repository;

import java.util.Optional;
import se.lexicon.eventapp.model.Participant;

public interface ParticipantRepository extends Repository<Participant> {

    Optional<Participant> findByEmail(String email);
}
