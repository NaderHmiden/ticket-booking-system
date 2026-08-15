package com.ticketapp.demo.catalog.internal;

import com.ticketapp.demo.common.event.DomainEvent;
import com.ticketapp.demo.common.event.KafkaTopics;

import java.util.UUID;

/**
 * Published when a TicketType is created for a GENERAL_ADMISSION event.
 * Consumed by Booking's read model to initialize ticket availability in Redis.
 * {@code aggregateId} is the ticketTypeId to ensure events for the same ticket type
 * are processed in order on the same partition.
 */
public final class TicketTypeCreated extends DomainEvent {

    private final UUID ticketTypeId;
    private final UUID eventId;
    private final int totalQuantity;

    public TicketTypeCreated(UUID ticketTypeId, UUID eventId, int totalQuantity) {
        super(ticketTypeId.toString());
        this.ticketTypeId = ticketTypeId;
        this.eventId = eventId;
        this.totalQuantity = totalQuantity;
    }

    public UUID getTicketTypeId() {
        return ticketTypeId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    @Override
    public String topic() {
        return KafkaTopics.CATALOG_TICKET_TYPE_CREATED;
    }
}
