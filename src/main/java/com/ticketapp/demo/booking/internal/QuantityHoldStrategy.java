package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.booking.AcquireHoldRequest;
import com.ticketapp.demo.booking.HoldResult;
import com.ticketapp.demo.booking.HoldStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hold strategy for GENERAL_ADMISSION events: reserves a quantity from shared stock via Lua script.
 * Unlike SeatHoldStrategy which reserves specific seats, QuantityHoldStrategy decrements a shared
 * counter in Redis. Each hold stores the quantity and ticket type ID so release can re-increment.
 */
@Component
public class QuantityHoldStrategy implements HoldStrategy {

    private static final String HOLD_KEY_PREFIX = "booking:quantity-hold:";
    private static final String TICKET_AVAILABLE_KEY_PREFIX = "booking:ticket-type-available:";

    /**
     * Lua script to atomically decrement ticket availability and record the hold.
     * KEYS[1]: ticket availability key
     * KEYS[2]: hold key
     * ARGV[1]: quantity requested
     * ARGV[2]: holdId (JSON with ticketTypeId, quantity)
     * ARGV[3]: TTL in seconds
     * Returns 1 if successful, 0 if insufficient inventory.
     */
    private static final RedisScript<Long> ACQUIRE_QUANTITY = new DefaultRedisScript<>(
            """
            local available = tonumber(redis.call("get", KEYS[1]) or "0")
            local requested = tonumber(ARGV[1])
            if available >= requested then
                redis.call("decrby", KEYS[1], requested)
                redis.call("setex", KEYS[2], ARGV[3], ARGV[2])
                return 1
            else
                return 0
            end
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final BookingEventPublisher eventPublisher;
    private final Duration holdTtl;

    public QuantityHoldStrategy(StringRedisTemplate redisTemplate,
                               BookingEventPublisher eventPublisher,
                               @Value("${booking.hold.ttl-seconds:300}") long holdTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.holdTtl = Duration.ofSeconds(holdTtlSeconds);
    }

    @Override
    public HoldResult acquire(AcquireHoldRequest request) {
        // For GENERAL_ADMISSION, the request must include ticketTypeId and quantity
        // For now, we'll extract from a special request type. The controller will handle this.
        throw new UnsupportedOperationException("QuantityHoldStrategy requires QuantityAcquireHoldRequest");
    }

    /**
     * Acquire a hold for a specific ticket type and quantity.
     * @param ticketTypeId the ticket type to reserve
     * @param quantity the number of tickets
     * @param eventId the event ID
     * @param userId the user making the reservation
     * @return HoldResult if successful
     * @throws IllegalStateException if quantity not available
     */
    public HoldResult acquireQuantity(UUID ticketTypeId, int quantity, UUID eventId, UUID userId) {
        UUID holdId = UUID.randomUUID();
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String ticketAvailableKey = TICKET_AVAILABLE_KEY_PREFIX + ticketTypeId;
        String holdData = ticketTypeId + ":" + quantity;

        Long result = redisTemplate.execute(
                ACQUIRE_QUANTITY,
                java.util.List.of(ticketAvailableKey, holdKey),
                String.valueOf(quantity),
                holdData,
                String.valueOf(holdTtl.toSeconds())
        );

        if (result == null || result != 1L) {
            throw new IllegalStateException("Insufficient ticket availability for ticket type " + ticketTypeId);
        }

        Instant expiresAt = Instant.now().plus(holdTtl);
        // Publish availability changed event (optional, for read model updates)
        String currentAvailable = redisTemplate.opsForValue().get(ticketAvailableKey);
        if (currentAvailable != null) {
            eventPublisher.publish(new TicketAvailabilityChanged(ticketTypeId, Integer.parseInt(currentAvailable)));
        }

        return new HoldResult(holdId, ticketTypeId, eventId, expiresAt);
    }

    @Override
    public boolean release(UUID seatId, UUID holdId) {
        // For quantity holds, seatId is actually the ticket type ID
        String holdKey = HOLD_KEY_PREFIX + holdId;
        String holdData = redisTemplate.opsForValue().get(holdKey);

        if (holdData == null) {
            return false; // Hold not found or already released
        }

        // Parse hold data: "ticketTypeId:quantity"
        String[] parts = holdData.split(":");
        if (parts.length != 2) {
            return false;
        }

        UUID ticketTypeId = UUID.fromString(parts[0]);
        int quantity = Integer.parseInt(parts[1]);

        String ticketAvailableKey = TICKET_AVAILABLE_KEY_PREFIX + ticketTypeId;

        // Re-increment availability (atomically using Lua)
        redisTemplate.execute(
                new DefaultRedisScript<>(
                        """
                        redis.call("incrby", KEYS[1], ARGV[1])
                        redis.call("del", KEYS[2])
                        return 1
                        """,
                        Long.class
                ),
                java.util.List.of(ticketAvailableKey, holdKey),
                String.valueOf(quantity)
        );

        // Publish availability changed event
        String currentAvailable = redisTemplate.opsForValue().get(ticketAvailableKey);
        if (currentAvailable != null) {
            eventPublisher.publish(new TicketAvailabilityChanged(ticketTypeId, Integer.parseInt(currentAvailable)));
        }

        return true;
    }

    @Override
    public boolean confirm(UUID seatId, UUID holdId) {
        // For quantity holds, just delete the hold key without re-incrementing
        // The quantity is already decremented and considered "sold"
        String holdKey = HOLD_KEY_PREFIX + holdId;
        Long deleted = redisTemplate.delete(Collections.singleton(holdKey));
        return deleted != null && deleted > 0;
    }
}
