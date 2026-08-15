package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.common.event.DomainEvent;
import com.ticketapp.demo.common.event.KafkaTopics;

import java.util.UUID;

/**
 * Published when ticket availability for a ticket type changes.
 * Consumed by Catalog's read model to update ticket availability display.
 * {@code aggregateId} is the ticketTypeId to ensure events for the same ticket type
 * are processed in order on the same partition.
 */
public final class TicketAvailabilityChanged extends DomainEvent {

    private final UUID ticketTypeId;
    private final int newAvailable;

    public TicketAvailabilityChanged(UUID ticketTypeId, int newAvailable) {
        super(ticketTypeId.toString());
        this.ticketTypeId = ticketTypeId;
        this.newAvailable = newAvailable;
    }

    public UUID getTicketTypeId() {
        return ticketTypeId;
    }

    public int getNewAvailable() {
        return newAvailable;
    }

    @Override
    public String topic() {
        return KafkaTopics.BOOKING_TICKET_AVAILABILITY_CHANGED;
    }
}
