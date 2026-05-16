package com.connecthub.room.model;

/**
 * Roles a member can hold inside a room.
 * OWNER  = created the room, can delete it
 * ADMIN  = can add/remove members, change settings
 * MEMBER = standard participant
 */
public enum MemberRole {
    OWNER,
    ADMIN,
    MEMBER
}
