package com.ticketapp.demo.booking.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Booking's domain events to Kafka. Deliberately fire-and-forget
 * from the caller's perspective: {@link } calls this AFTER
 * the Redis operation that actually determines the outcome has already
 * succeeded. If Kafka is slow or briefly unavailable, that must never turn
 * into a failed hold — the hold already happened, correctly, in Redis. A
 * lost event here degrades Catalog's browsing accuracy (see Step 2's
 * consistency notes); it must never degrade Booking's own guarantee.
 */
@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BookingEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            // Keyed by aggregateId (the seatId) so same-seat events land on
            // the same partition and are processed in publish order by any
            // consumer — see SeatHeldEvent's Javadoc for why that matters.
            kafkaTemplate.send(event.topic(), event.getAggregateId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish {} for aggregate {}: {}",
                                    event.getClass().getSimpleName(), event.getAggregateId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            // Serialization failure would be a programming error, not a
            // transient infra issue — still must not propagate and fail
            // the hold/release that already succeeded in Redis.
            log.error("Failed to serialize {} for aggregate {}", event.getClass().getSimpleName(),
                    event.getAggregateId(), e);
        }
    }
}
