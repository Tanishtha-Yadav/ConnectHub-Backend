package com.connecthub.room.model;

/**
 * Defines the kind of room.
 * DIRECT  = one-on-one DM between exactly two users
 * GROUP   = multi-user group channel
 * CHANNEL = public or topic-based broadcast channel
 */
public enum RoomType {
    DIRECT,
    GROUP,
    CHANNEL
}
