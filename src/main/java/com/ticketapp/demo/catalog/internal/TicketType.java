package com.ticketapp.demo.catalog.internal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ticket_type")
public class TicketType {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    protected TicketType() { } // JPA

    public TicketType(UUID id, UUID eventId, String name, BigDecimal price, int totalQuantity) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getTotalQuantity() { return totalQuantity; }
}
