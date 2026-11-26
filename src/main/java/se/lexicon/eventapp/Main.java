package se.lexicon.eventapp;

import se.lexicon.eventapp.db.DatabaseInitializer;
import se.lexicon.eventapp.exception.AppException;
import se.lexicon.eventapp.repository.EventRepository;
import se.lexicon.eventapp.repository.InvitationRepository;
import se.lexicon.eventapp.repository.JdbcEventRepository;
import se.lexicon.eventapp.repository.JdbcInvitationRepository;
import se.lexicon.eventapp.repository.JdbcParticipantRepository;
import se.lexicon.eventapp.repository.ParticipantRepository;
import se.lexicon.eventapp.service.EventService;
import se.lexicon.eventapp.service.InvitationService;
import se.lexicon.eventapp.service.ParticipantService;
import se.lexicon.eventapp.ui.ConsoleMenu;

/**
 * Starts the application: creates the schema, wires the objects together and opens the menu.
 */
public class Main {

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();

            ParticipantRepository participantRepository = new JdbcParticipantRepository();
            EventRepository eventRepository = new JdbcEventRepository();
            InvitationRepository invitationRepository = new JdbcInvitationRepository();

            ParticipantService participantService = new ParticipantService(participantRepository);
            EventService eventService = new EventService(eventRepository, invitationRepository);
            InvitationService invitationService =
                    new InvitationService(invitationRepository, eventRepository, participantRepository);

            new ConsoleMenu(participantService, eventService, invitationService).run();
        } catch (AppException e) {
            System.err.println("The application could not start: " + e.getMessage());
            System.err.println("Check that MySQL is running and that db.properties matches your database.");
        }
    }
}
