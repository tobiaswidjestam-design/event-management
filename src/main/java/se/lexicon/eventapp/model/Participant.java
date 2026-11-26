package se.lexicon.eventapp.model;

import se.lexicon.eventapp.exception.ValidationException;

/**
 * A participant of an event: either a private person or a representative of an organization.
 * The object validates itself so an invalid participant can never be created.
 */
public class Participant {

    private int id;
    private String name;
    private String email;
    private ParticipantType type;
    private String organization;

    /**
     * Creates a new participant and validates the input. The organization name is kept only
     * for ORGANIZATION participants, and is required for them.
     */
    public Participant(String name, String email, ParticipantType type, String organization) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Participant name is required");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("Participant email is required");
        }
        if (type == null) {
            throw new ValidationException("Participant type is required");
        }
        if (type == ParticipantType.ORGANIZATION && (organization == null || organization.isBlank())) {
            throw new ValidationException("An organization participant must have an organization name");
        }

        this.name = name.trim();
        this.email = email.trim();
        this.type = type;
        this.organization = type == ParticipantType.ORGANIZATION ? organization.trim() : null;
    }

    /**
     * Used by the repository when a participant is read from the database.
     */
    public Participant(int id, String name, String email, ParticipantType type, String organization) {
        this(name, email, type, organization);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public ParticipantType getType() {
        return type;
    }

    public String getOrganization() {
        return organization;
    }

    /**
     * True for a company or organization, used when the participant is printed in the console.
     */
    public boolean isOrganization() {
        return type == ParticipantType.ORGANIZATION;
    }

    @Override
    public String toString() {
        if (isOrganization()) {
            return String.format("#%d %s (%s, %s) <%s>", id, name, type, organization, email);
        }
        return String.format("#%d %s (%s) <%s>", id, name, type, email);
    }
}
