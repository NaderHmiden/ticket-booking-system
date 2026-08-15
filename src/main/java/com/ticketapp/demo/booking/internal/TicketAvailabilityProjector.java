package com.ticketapp.demo.booking.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.common.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes Catalog's TicketTypeCreated events to initialize ticket availability
 * in Redis for GENERAL_ADMISSION events. Each ticket type gets a Redis key
 * ticket_type:{ticketTypeId}:available with the total quantity.
 */
@Component
public class TicketAvailabilityProjector {

    private static final Logger log = LoggerFactory.getLogger(TicketAvailabilityProjector.class);
    private static final String TICKET_AVAILABLE_KEY_PREFIX = "booking:ticket-type-available:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TicketAvailabilityProjector(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.CATALOG_TICKET_TYPE_CREATED, groupId = "booking-ticket-availability-projector")
    public void onTicketTypeCreated(String message) {
        try {
            var payload = objectMapper.readValue(message, CatalogEventPayloads.TicketTypeCreated.class);
            String key = TICKET_AVAILABLE_KEY_PREFIX + payload.ticketTypeId();
            redisTemplate.opsForValue().set(key, String.valueOf(payload.totalQuantity()));
            log.info("Initialized ticket availability for ticket type {} with {} tickets",
                    payload.ticketTypeId(), payload.totalQuantity());
        } catch (Exception e) {
            log.warn("Skipping malformed catalog.ticket-type-created message: {}", message, e);
        }
    }
}
