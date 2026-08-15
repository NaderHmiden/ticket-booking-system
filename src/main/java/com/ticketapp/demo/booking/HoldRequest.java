package com.ticketapp.demo.booking;

import java.time.Instant;
import java.util.UUID;

/**
 * Base request for acquiring a hold. Subclasses will specify seat-based or quantity-based holds.
 */
public abstract class HoldRequest {
    protected final UUID eventId;
    protected final UUID userId;

    protected HoldRequest(UUID eventId, UUID userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }
}
