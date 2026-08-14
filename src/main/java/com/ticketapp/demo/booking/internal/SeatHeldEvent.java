package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.common.event.DomainEvent;
import com.ticketapp.demo.common.event.KafkaTopics;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a seat hold is successfully acquired. Consumed by Catalog's
 * {@code CatalogReadModelProjector} — which does NOT import this class (see
 * that class's Javadoc); this type only ever gets as far as the Kafka
 * producer, serialized to JSON by {@link BookingEventPublisher}.
 *
 * <p>{@code aggregateId} is the seatId, not a holdId or bookingId — this is
 * what guarantees Kafka partitions (and therefore processing order) by
 * seat, which is the property {@code CatalogReadModelProjector} depends on
 * to never process a release before its matching hold.
 */
public final class SeatHeldEvent extends DomainEvent {

    private final UUID seatId;
    private final Instant expiresAt;

    public SeatHeldEvent(UUID seatId, Instant expiresAt) {
        super(seatId.toString());
        this.seatId = seatId;
        this.expiresAt = expiresAt;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String topic() {
        return KafkaTopics.BOOKING_SEAT_HELD;
    }
}
