package com.ticketapp.demo.catalog.internal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Catalog's own local view of the JSON events Booking is expected to publish
 * (per the contract documented in {@link com.ticketbooking.common.event.KafkaTopics}).
 * These records deliberately do NOT reuse a shared Java event class from the
 * Booking module — that would make Catalog depend on Booking's public
 * surface, which {@code ModuleBoundaryTest} forbids, and more importantly
 * would recouple the two contexts through code instead of through a wire
 * format. {@code @JsonIgnoreProperties(ignoreUnknown = true)} means Catalog
 * only pulls the two or three fields it actually cares about and silently
 * tolerates whatever else Booking's real {@code DomainEvent} envelope
 * includes (eventId, occurredAt, aggregateId, bookingId, userId, ...).
 */
final class BookingLifecycleEventPayloads {

    private BookingLifecycleEventPayloads() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeatHeld(UUID seatId, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeatReleased(UUID seatId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeatBooked(UUID seatId) {
    }
}

