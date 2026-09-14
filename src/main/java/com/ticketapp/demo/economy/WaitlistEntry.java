package com.ticketapp.demo.economy;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "waitlist_entry", uniqueConstraints = @UniqueConstraint(name = "uq_waitlist_event_user_tier", columnNames = {"event_id", "user_id", "tier"}))
public class WaitlistEntry {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String tier;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected WaitlistEntry() { }
    public WaitlistEntry(UUID id, UUID eventId, UUID userId, String tier) { this.id = id; this.eventId = eventId; this.userId = userId; this.tier = tier; this.createdAt = Instant.now(); }
    public UUID getEventId() { return eventId; }
    public String getTier() { return tier; }
    public Instant getCreatedAt() { return createdAt; }
}
