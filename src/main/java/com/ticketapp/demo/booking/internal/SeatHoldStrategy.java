package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.booking.AcquireHoldRequest;
import com.ticketapp.demo.booking.HoldResult;
import com.ticketapp.demo.booking.HoldStrategy;
import com.ticketapp.demo.booking.SeatAlreadyHeldException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Hold strategy for SEATED events: acquires individual seats via Redis SET NX.
 * Extracted from SeatHoldService to support multiple hold strategies.
 */
@Component
public class SeatHoldStrategy implements HoldStrategy {

    private final RedisSeatHoldStore holdStore;
    private final BookingEventPublisher eventPublisher;
    private final Duration holdTtl;

    public SeatHoldStrategy(RedisSeatHoldStore holdStore,
                           BookingEventPublisher eventPublisher,
                           @Value("${booking.hold.ttl-seconds:300}") long holdTtlSeconds) {
        this.holdStore = holdStore;
        this.eventPublisher = eventPublisher;
        this.holdTtl = Duration.ofSeconds(holdTtlSeconds);
    }

    @Override
    public HoldResult acquire(AcquireHoldRequest request) {
        UUID holdId = holdStore.acquire(request.seatId(), holdTtl)
                .orElseThrow(() -> new SeatAlreadyHeldException(request.seatId()));

        Instant expiresAt = Instant.now().plus(holdTtl);
        eventPublisher.publish(new SeatHeldEvent(request.seatId(), expiresAt));

        return new HoldResult(holdId, request.seatId(), request.eventId(), expiresAt);
    }

    @Override
    public boolean release(UUID seatId, UUID holdId) {
        boolean released = holdStore.release(seatId, holdId);
        if (released) {
            eventPublisher.publish(new SeatReleasedEvent(seatId));
        }
        return released;
    }

    @Override
    public boolean confirm(UUID seatId, UUID holdId) {
        // For seats, confirmation means deleting the hold without modifying the seat status.
        // The seat's status is permanently "booked" in Redis once the order is confirmed.
        return holdStore.isCurrentHolder(seatId, holdId);
    }
}
