package se.lexicon.eventapp.service;

import java.util.List;
import se.lexicon.eventapp.exception.NotFoundException;
import se.lexicon.eventapp.exception.ValidationException;
import se.lexicon.eventapp.model.Participant;
import se.lexicon.eventapp.model.ParticipantType;
import se.lexicon.eventapp.repository.ParticipantRepository;

/**
 * Use cases for participants: registration and lookup.
 */
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    public ParticipantService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    /**
     * Registers a participant. The email is the identity of a person or organization,
     * so a second registration with the same email is refused.
     */
    public Participant register(String name, String email, ParticipantType type, String organization) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Participant email is required");
        }

        participantRepository.findByEmail(email.trim()).ifPresent(existing -> {
            throw new ValidationException(
                    "A participant with email " + existing.getEmail() + " is already registered");
        });

        Participant participant = new Participant(name, email, type, organization);
        return participantRepository.save(participant);
    }

    /**
     * All participants that are registered, in the order the repository returns them.
     */
    public List<Participant> findAll() {
        return participantRepository.findAll();
    }

    /**
     * Loads one participant, or throws NotFoundException when the id does not exist.
     */
    public Participant findById(int id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No participant with id " + id));
    }
}
