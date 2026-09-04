package com.ticketapp.demo.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_sections")
public class EventSection {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_row", nullable = false)
    private int startRow;

    @Column(name = "end_row", nullable = false)
    private int endRow;

    @Column(name = "start_col", nullable = false)
    private int startCol;

    @Column(name = "end_col", nullable = false)
    private int endCol;

    @Column(nullable = false)
    private String color;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EventSection() { }

    public EventSection(UUID id, UUID eventId, CreateSectionRequest request) {
        this.id = id;
        this.eventId = eventId;
        this.name = request.name().trim();
        this.startRow = request.startRow();
        this.endRow = request.endRow();
        this.startCol = request.startCol();
        this.endCol = request.endCol();
        this.color = request.color().trim();
        this.priceCents = request.priceCents();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getName() { return name; }
    public int getStartRow() { return startRow; }
    public int getEndRow() { return endRow; }
    public int getStartCol() { return startCol; }
    public int getEndCol() { return endCol; }
    public String getColor() { return color; }
    public int getPriceCents() { return priceCents; }
    public Instant getCreatedAt() { return createdAt; }
}
