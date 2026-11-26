package se.lexicon.eventapp.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import se.lexicon.eventapp.exception.AppException;
import se.lexicon.eventapp.model.Event;
import se.lexicon.eventapp.model.Invitation;
import se.lexicon.eventapp.model.Participant;
import se.lexicon.eventapp.model.ParticipantType;
import se.lexicon.eventapp.service.EventService;
import se.lexicon.eventapp.service.InvitationService;
import se.lexicon.eventapp.service.ParticipantService;

/**
 * The console user interface. It only handles input and output: every rule lives in the services,
 * and every expected error is caught here so that the menu keeps running.
 */
public class ConsoleMenu {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner scanner;
    private final ParticipantService participantService;
    private final EventService eventService;
    private final InvitationService invitationService;

    public ConsoleMenu(ParticipantService participantService, EventService eventService,
                       InvitationService invitationService) {
        this(new Scanner(System.in), participantService, eventService, invitationService);
    }

    public ConsoleMenu(Scanner scanner, ParticipantService participantService,
                       EventService eventService, InvitationService invitationService) {
        this.scanner = scanner;
        this.participantService = participantService;
        this.eventService = eventService;
        this.invitationService = invitationService;
    }

    public void run() {
        boolean running = true;

        while (running) {
            printMenu();
            String choice = readLine("Choose: ");

            try {
                running = handle(choice);
            } catch (AppException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a number");
            } catch (DateTimeParseException e) {
                System.out.println("Error: use the format yyyy-MM-dd HH:mm, for example 2026-12-01 18:00");
            }
            System.out.println();
        }

        System.out.println("Goodbye!");
    }

    private boolean handle(String choice) {
        switch (choice) {
            case "1" -> registerParticipant();
            case "2" -> createEvent();
            case "3" -> inviteParticipant();
            case "4" -> answerInvitation();
            case "5" -> listUpcomingEvents();
            case "6" -> listInvitationsForEvent();
            case "7" -> listEventsForParticipant();
            case "8" -> showAttendance();
            case "9" -> showAvailableSeats();
            case "0" -> {
                return false;
            }
            default -> System.out.println("Error: choose a number between 0 and 9");
        }
        return true;
    }

    private void printMenu() {
        System.out.println("===== Event Management =====");
        System.out.println("1. Register participant (individual or organization)");
        System.out.println("2. Create event");
        System.out.println("3. Invite participant to event");
        System.out.println("4. Accept or decline an invitation");
        System.out.println("5. List upcoming events");
        System.out.println("6. List invitations for an event");
        System.out.println("7. List events for a participant");
        System.out.println("8. Show attendance list (accepted only)");
        System.out.println("9. Show available seats for an event");
        System.out.println("0. Exit");
    }

    private void registerParticipant() {
        String name = readLine("Name: ");
        String email = readLine("Email: ");
        System.out.println("1. Individual");
        System.out.println("2. Organization");
        String typeChoice = readLine("Type: ");

        ParticipantType type;
        String organization = null;

        if ("2".equals(typeChoice)) {
            type = ParticipantType.ORGANIZATION;
            organization = readLine("Organization name: ");
        } else {
            type = ParticipantType.INDIVIDUAL;
        }

        Participant participant = participantService.register(name, email, type, organization);
        System.out.println("Registered: " + participant);
    }

    private void createEvent() {
        String title = readLine("Title: ");
        String description = readLine("Description (optional): ");
        LocalDateTime start = readDateTime("Start (yyyy-MM-dd HH:mm): ");
        LocalDateTime end = readDateTime("End   (yyyy-MM-dd HH:mm): ");
        String location = readLine("Location: ");
        int capacity = readInt("Capacity: ");

        Event event = eventService.createEvent(title, description, start, end, location, capacity);
        System.out.println("Created: " + event);
    }

    private void inviteParticipant() {
        listParticipants();
        int eventId = readInt("Event id: ");
        int participantId = readInt("Participant id: ");

        Invitation invitation = invitationService.invite(eventId, participantId);
        System.out.println("Invited: " + invitation);
    }

    private void answerInvitation() {
        int eventId = readInt("Event id: ");
        List<Invitation> invitations = invitationService.invitationsFor(eventId);

        if (invitations.isEmpty()) {
            System.out.println("No invitations for event " + eventId);
            return;
        }

        invitations.forEach(invitation -> System.out.println(
                "  " + invitation.getId() + ") " + invitation.getParticipant().getName()
                        + " [" + invitation.getStatus() + "]"));

        int invitationId = readInt("Invitation id: ");
        String action = readLine("Accept or decline? (a/d): ");

        if ("d".equalsIgnoreCase(action)) {
            invitationService.decline(invitationId);
            System.out.println("Invitation declined");
        } else if ("a".equalsIgnoreCase(action)) {
            invitationService.accept(invitationId);
            System.out.println("Invitation accepted");
        } else {
            System.out.println("Nothing changed: answer with a or d");
        }
    }

    private void listUpcomingEvents() {
        List<Event> events = eventService.findUpcoming();

        if (events.isEmpty()) {
            System.out.println("There are no upcoming events");
            return;
        }

        events.forEach(event -> System.out.println("  " + event));
    }

    private void listInvitationsForEvent() {
        int eventId = readInt("Event id: ");
        List<Invitation> invitations = invitationService.invitationsFor(eventId);

        if (invitations.isEmpty()) {
            System.out.println("No invitations for event " + eventId);
            return;
        }

        invitations.forEach(invitation -> System.out.println("  " + invitation));
    }

    private void listEventsForParticipant() {
        listParticipants();
        int participantId = readInt("Participant id: ");
        List<Event> events = invitationService.eventsForParticipant(participantId);

        if (events.isEmpty()) {
            System.out.println("This participant has no invitations");
            return;
        }

        events.forEach(event -> System.out.println("  " + event));
    }

    private void showAttendance() {
        int eventId = readInt("Event id: ");
        List<Participant> attendees = invitationService.attendeesFor(eventId);

        System.out.println("Attending (" + attendees.size() + "):");
        attendees.forEach(participant -> System.out.println("  " + participant));
    }

    private void showAvailableSeats() {
        int eventId = readInt("Event id: ");
        Event event = eventService.findById(eventId);
        System.out.println("Seats left for \"" + event.getTitle() + "\": " + eventService.availableSeats(eventId));
    }

    private void listParticipants() {
        List<Participant> participants = participantService.findAll();

        if (participants.isEmpty()) {
            System.out.println("No participants registered yet");
            return;
        }

        participants.forEach(participant -> System.out.println("  " + participant));
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "0";
        }
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        String value = readLine(prompt);
        return Integer.parseInt(value);
    }

    private LocalDateTime readDateTime(String prompt) {
        String value = readLine(prompt);
        return LocalDateTime.parse(value, DATE_TIME);
    }
}
