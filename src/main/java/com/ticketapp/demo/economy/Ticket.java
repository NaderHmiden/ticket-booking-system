package com.ticketapp.demo.economy;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket")
public class Ticket {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "resource_id") private UUID resourceId;
    @Column(nullable = false) private String tier;
    @Column(name = "price_cents", nullable = false) private int priceCents;
    @Column(name = "seat_label") private String seatLabel;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TicketStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Ticket() { }
    public Ticket(UUID id, UUID eventId, UUID ownerId, UUID resourceId, String tier, int priceCents, String seatLabel) {
        this.id = id; this.eventId = eventId; this.ownerId = ownerId; this.resourceId = resourceId;
        this.tier = tier; this.priceCents = priceCents; this.seatLabel = seatLabel;
        this.status = TicketStatus.VALID; this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getResourceId() { return resourceId; }
    public String getTier() { return tier; }
    public int getPriceCents() { return priceCents; }
    public String getSeatLabel() { return seatLabel; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void listForResale() { if (status != TicketStatus.VALID) throw new IllegalStateException("Only valid tickets can be listed for resale"); status = TicketStatus.RESALE_PENDING; updatedAt = Instant.now(); }
}
