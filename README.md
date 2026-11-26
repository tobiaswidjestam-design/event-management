# Event Management App

A Java SE console application for a community center. Staff create events, invite participants and follow up
on who is attending. All data is persisted in MySQL through JDBC.

This project is the solution to **Project Test 1 (Event Management App)** in the Lexicon course
*Software Developer – Java*.

## Features

| # | Feature |
|---|---------|
| 1 | Register participants (individuals and organizations) |
| 2 | Create events with title, description, time range, location and capacity |
| 3 | Invite participants to an event |
| 4 | Accept or decline an invitation |
| 5 | Prevent duplicate invitations for the same event |
| 6 | Prevent overbooking (accepted participants cannot exceed the capacity) |
| 7 | List upcoming events |
| 8 | List invitations for a specific event, with status |
| 9 | List events for a specific participant |
| 10 | Attendance list (accepted participants only) and available seats per event |
| 11 | Store and load everything with SQL + JDBC |

## Requirements

- JDK 21 or newer
- Maven 3.9 or newer
- MySQL 8.0 or newer

## Setup

### 1. Create the database and a user

```sql
CREATE DATABASE event_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'eventapp'@'localhost' IDENTIFIED BY 'eventapp';
GRANT ALL PRIVILEGES ON event_management.* TO 'eventapp'@'localhost';
FLUSH PRIVILEGES;
```

Only the empty database has to exist. The tables are created by the application on startup,
`DatabaseInitializer` runs `src/main/resources/schema.sql`.

### 2. Check the connection settings

`src/main/resources/db.properties` holds the URL, user and password used locally:

```properties
db.url=jdbc:mysql://127.0.0.1:3306/event_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=eventapp
db.password=eventapp
```

Change these values if your MySQL installation uses another port or another user.

### 3. Build and run

```bash
mvn clean package
mvn exec:java
```

`mvn exec:java` starts the console menu. The menu numbers are the same as the feature list above.

### 4. Run the tests

```bash
mvn test
```

The tests cover the domain rules (validation and invitation state) and the booking rules in `InvitationService`.
They use in-memory fakes for the repositories, so no database is needed to run them.

## Using the application

```
1. Register participant (individual or organization)
2. Create event
3. Invite participant to event
4. Accept or decline an invitation
5. List upcoming events
6. List invitations for an event
7. List events for a participant
8. Show attendance list (accepted only)
9. Show available seats for an event
0. Exit
```

Dates and times are entered as `yyyy-MM-dd HH:mm`, for example `2026-12-01 18:00`.

## Domain rules

| # | Rule |
|---|------|
| R1 | An event needs a title, a positive capacity and `start < end` |
| R2 | The same participant cannot be invited twice to the same event |
| R3 | The number of accepted invitations can never exceed the capacity |
| R4 | An invitation only moves PENDING → ACCEPTED or PENDING → DECLINED |
| R5 | Participants cannot be invited to an event that has already ended |

## Architecture

```
ui (ConsoleMenu)  ->  service (EventService, ParticipantService, InvitationService)
                  ->  repository (interfaces + JDBC implementations)
                  ->  model (Event, Participant, Invitation, enums)
                      db (ConnectionFactory, DatabaseInitializer)
```

- **model** contains the domain objects. They validate themselves and own their behaviour (`accept()`,
  `decline()`, `hasFreeSeat()`), which keeps the rules in one place.
- **repository** is defined by a generic `Repository<T>` interface with three JDBC implementations.
  The services depend on the interfaces, not on MySQL, which is why they can be tested with in-memory fakes.
- **service** contains the use cases and the business rules that span several objects (duplicate invitations,
  overbooking, upcoming events).
- **ui** is a plain console menu. It catches `AppException` and prints a readable message, so invalid input
  never crashes the application.

## UML class diagram

```
              <<interface>>  Repository<T>
              + findAll(): List<T>
              + findById(int): Optional<T>
              + save(T): T
              + delete(int): boolean
                       ^
     +-----------------+------------------+
     |                 |                  |
 JdbcParticipant-  JdbcEvent-      JdbcInvitation-
 Repository        Repository      Repository

 <<enum>> ParticipantType          <<enum>> InvitationStatus
   INDIVIDUAL | ORGANIZATION         PENDING | ACCEPTED | DECLINED

 Participant                         Invitation
 - id: int                           - id: int
 - name: String                      - event: Event
 - email: String                     - participant: Participant
 - type: ParticipantType             - status: InvitationStatus
 - organization: String              - invitedAt: LocalDateTime
 + isOrganization(): boolean         + accept(): void
                                     + decline(): void
 Event                               + isAccepted(): boolean
 - id: int
 - title: String                 EventService        : createEvent, upcomingEvents, availableSeats
 - description: String           ParticipantService  : register, findAll, findById
 - start, end: LocalDateTime     InvitationService   : invite, accept, decline, attendeesFor, eventsForParticipant
 - location: String              ConsoleMenu         : menu loop and input handling
 - capacity: int                 DatabaseInitializer : creates the schema on startup
 + hasFreeSeat(int accepted): boolean
 + hasEnded(): boolean
```

Relationships: `Invitation` has one `Event` and one `Participant`. The database enforces this with two foreign
keys and a unique constraint on `(event_id, participant_id)`, so rule R2 holds even outside the application.

## Database tables

| Table | Purpose |
|-------|---------|
| `participants` | individuals and organizations, with a unique email |
| `events` | title, description, time range, location, capacity |
| `invitations` | link between event and participant, with status and invite time |

## Git workflow

The project was built with small commits, one per feature area, and one feature branch (`feature/invitations`)
that was merged back into `main`:

```
chore: initialize Maven project and package structure
feat(model): add domain classes, enums and exceptions
feat(db): add schema and JDBC connection factory
feat(repository): add participant, event and invitation repositories
feat(service): enforce invitation and capacity rules
feat(ui): add console menu and application entry point
test: add JUnit tests for invitation rules
```
