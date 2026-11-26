-- Schema for the Event Management App.
-- Executed by DatabaseInitializer on startup, so the database only has to exist.

CREATE TABLE IF NOT EXISTS participants (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    type         ENUM('INDIVIDUAL', 'ORGANIZATION') NOT NULL,
    organization VARCHAR(150) NULL
);

CREATE TABLE IF NOT EXISTS events (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    description TEXT NULL,
    start_time  DATETIME NOT NULL,
    end_time    DATETIME NOT NULL,
    location    VARCHAR(150) NOT NULL,
    capacity    INT NOT NULL,
    CONSTRAINT chk_event_capacity CHECK (capacity > 0),
    CONSTRAINT chk_event_time CHECK (end_time > start_time)
);

CREATE TABLE IF NOT EXISTS invitations (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    event_id       INT NOT NULL,
    participant_id INT NOT NULL,
    status         ENUM('PENDING', 'ACCEPTED', 'DECLINED') NOT NULL DEFAULT 'PENDING',
    invited_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invitation_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitation_participant FOREIGN KEY (participant_id) REFERENCES participants (id) ON DELETE CASCADE,
    CONSTRAINT uq_invitation UNIQUE (event_id, participant_id)
);
