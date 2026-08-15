package com.ticketapp.demo.catalog.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Catalog's domain events to Kafka. Like Booking's publisher,
 * this is fire-and-forget: if Kafka is unavailable, the database commit
 * has already succeeded, and we must not fail the operation.
 */
@Component
public class CatalogEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CatalogEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(event.topic(), event.getAggregateId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to publish {} for aggregate {}: {}",
                                    event.getClass().getSimpleName(), event.getAggregateId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize {} for aggregate {}", event.getClass().getSimpleName(),
                    event.getAggregateId(), e);
        }
    }
}
