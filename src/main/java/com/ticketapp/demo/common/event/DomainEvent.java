package com.ticketapp.demo.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base envelope for every event that crosses a bounded-context boundary via Kafka.
 *
 * <p>Two fields matter more than the rest here:
 * <ul>
 *   <li>{@code eventId} — a stable UUID assigned at creation time (not by Kafka).
 *       Consumers use this as the idempotency key: Kafka is at-least-once, so every
 *       consumer must be able to say "have I already processed this eventId?" and
 *       skip if so. See each context's {@code ProcessedEvent} tracking table.</li>
 *   <li>{@code aggregateId} — the id of the aggregate the event is about (e.g. the
 *       bookingId or seatId), used for partition keying so events about the same
 *       aggregate are strictly ordered on the same partition.</li>
 * </ul>
 *
 * <p>This class intentionally carries no domain semantics — subclasses (defined
 * within each bounded context, not here) add the actual payload.
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this(UUID.randomUUID(), Instant.now(), aggregateId);
    }

    protected DomainEvent(UUID eventId, Instant occurredAt, String aggregateId) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.aggregateId = aggregateId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * The Kafka topic this event is published to. Each concrete event owns this
     * so producers can't accidentally publish to the wrong topic.
     */
    public abstract String topic();
}
