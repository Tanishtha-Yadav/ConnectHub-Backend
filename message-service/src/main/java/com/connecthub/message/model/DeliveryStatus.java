package com.connecthub.message.model;

/**
 * Enum representing the delivery status of a message.
 * Messages transition: SENT → DELIVERED → READ
 */
public enum DeliveryStatus {
    SENT,       // Message saved to DB
    DELIVERED,  // Recipient WebSocket session is active
    READ        // Recipient has read the message (read receipt received)
}
