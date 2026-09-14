package com.ticketapp.demo.catalog.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_event")
public class EventListing {

    @Id
    private UUID id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "organizer_id")
    private UUID organizerId;

    @Column(name = "arrival_venue_id")
    private UUID arrivalVenueId;

    @Column(name = "arrives_at")
    private Instant arrivesAt;

    @Column(name = "transport_company")
    private String transportCompany;

    protected EventListing() { } // JPA

    public EventListing(UUID id, UUID venueId, String name, String category, Instant startsAt) {
        this(id, venueId, name, category, startsAt, EventType.SEATED);
    }

    public EventListing(UUID id, UUID venueId, String name, String category, Instant startsAt, EventType eventType) {
        this(id, venueId, name, category, startsAt, eventType, null);
    }

    public EventListing(UUID id, UUID venueId, String name, String category, Instant startsAt, EventType eventType, UUID organizerId) {
        this.id = id;
        this.venueId = venueId;
        this.name = name;
        this.category = category;
        this.startsAt = startsAt;
        this.status = EventStatus.ON_SALE;
        this.eventType = eventType != null ? eventType : EventType.SEATED;
        this.organizerId = organizerId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getVenueId() { return venueId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Instant getStartsAt() { return startsAt; }
    public EventStatus getStatus() { return status; }
    public EventType getEventType() { return eventType; }
    public UUID getOrganizerId() { return organizerId; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getArrivalVenueId() { return arrivalVenueId; }
    public Instant getArrivesAt() { return arrivesAt; }
    public String getTransportCompany() { return transportCompany; }
    public void setArrivalVenueId(UUID arrivalVenueId) { this.arrivalVenueId = arrivalVenueId; }
    public void setArrivesAt(Instant arrivesAt) { this.arrivesAt = arrivesAt; }
    public void setTransportCompany(String transportCompany) { this.transportCompany = transportCompany; }
}
