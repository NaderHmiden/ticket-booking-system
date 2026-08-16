package com.ticketapp.demo.booking.internal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Booking entity: tracks hold status through the payment lifecycle.
 * Status flows: HELD → PAYMENT_PENDING → (CONFIRMED | RELEASED)
 */
@Entity
@Table(name = "booking_hold")
public class Booking {

    @Id
    private UUID id;

    @Column(name = "hold_id", nullable = false, unique = true)
    private String holdId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_requested", nullable = false)
    private boolean paymentRequested;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Booking() { }

    public Booking(UUID id, String holdId, UUID eventId, UUID resourceId, UUID userId, BigDecimal amount) {
        this.id = id;
        this.holdId = holdId;
        this.eventId = eventId;
        this.resourceId = resourceId;
        this.userId = userId;
        this.amount = amount;
        this.status = "HELD";
        this.paymentRequested = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getHoldId() { return holdId; }
    public UUID getEventId() { return eventId; }
    public UUID getResourceId() { return resourceId; }
    public UUID getUserId() { return userId; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public boolean isPaymentRequested() { return paymentRequested; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setPaymentRequested(boolean requested) {
        this.paymentRequested = requested;
        this.updatedAt = Instant.now();
    }
}
