package com.connecthub.auth.model;

/**
 * User online status enumeration for presence tracking
 */
public enum UserStatus {
    ONLINE,      // User is actively using the application
    AWAY,        // User is idle but application is still open
    DND,         // Do Not Disturb - user is online but not available
    INVISIBLE    // User appears offline but is still connected
}
