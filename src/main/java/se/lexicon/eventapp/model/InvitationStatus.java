package se.lexicon.eventapp.model;

/**
 * The lifecycle of an invitation. An invitation starts as PENDING and can only move
 * once, to ACCEPTED or DECLINED.
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
