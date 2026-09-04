package com.ticketapp.demo.catalog.internal;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ticketapp.demo.common.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class CatalogReadModelProjector {

    private static final Logger log = LoggerFactory.getLogger(CatalogReadModelProjector.class);

    private final SeatAvailabilityReadModel readModel;
    private final ObjectMapper objectMapper;

    public CatalogReadModelProjector(SeatAvailabilityReadModel readModel, ObjectMapper objectMapper) {
        this.readModel = readModel;
        this.objectMapper = objectMapper;
    }
/*#######################################################*/
    @KafkaListener(topics = KafkaTopics.BOOKING_SEAT_HELD, groupId = "catalog-read-model-projector")
    public void onSeatHeld(String message) {
        try {
            var payload = objectMapper.readValue(message, BookingLifecycleEventPayloads.SeatHeld.class);
            Duration remainingTtl = Duration.between(Instant.now(), payload.expiresAt());
            readModel.markHeld(payload.seatId(), remainingTtl);
        } catch (Exception e) {
            log.warn("Skipping malformed booking.seat-held message: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_SEAT_RELEASED, groupId = "catalog-read-model-projector")
    public void onSeatReleased(String message) {
        try {
            var payload = objectMapper.readValue(message, BookingLifecycleEventPayloads.SeatReleased.class);
            readModel.clearHold(payload.seatId());
        } catch (Exception e) {
            log.warn("Skipping malformed booking.seat-released message: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CONFIRMED, groupId = "catalog-read-model-projector")
    public void onBookingConfirmed(String message) {
        try {
            var payload = objectMapper.readValue(message, BookingLifecycleEventPayloads.SeatBooked.class);
            readModel.markBooked(payload.seatId());
        } catch (Exception e) {
            log.warn("Skipping malformed booking.confirmed message: {}", message, e);
        }
    }
}
