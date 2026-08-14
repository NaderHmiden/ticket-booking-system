package com.ticketapp.demo.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "catalog_seat")
public class Seat {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String section;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false)
    private String currency;

    protected Seat() { } // JPA

    public Seat(UUID id, UUID eventId, String section, String rowLabel, int seatNumber,
                int priceCents, String currency) {
        this.id = id;
        this.eventId = eventId;
        this.section = section;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.priceCents = priceCents;
        this.currency = currency;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getSection() { return section; }
    public String getRowLabel() { return rowLabel; }
    public int getSeatNumber() { return seatNumber; }
    public int getPriceCents() { return priceCents; }
    public String getCurrency() { return currency; }
}
