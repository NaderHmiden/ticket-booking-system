package com.ticketapp.demo.notification.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.common.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Stub notification consumer: logs a fake email when a booking is confirmed.
 */
@Component
public class BookingConfirmedNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingConfirmedNotificationListener.class);

    private final ObjectMapper objectMapper;

    public BookingConfirmedNotificationListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CONFIRMED, groupId = "notification-booking-confirmed")
    public void onBookingConfirmed(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String holdId = text(node, "holdId");
            String seatId = text(node, "seatId");
            String userId = text(node, "userId");
            String amount = text(node, "amount");
            log.info(
                    "FAKE_EMAIL to userId={} — booking confirmed holdId={} seat/resourceId={} amount={}",
                    userId.isBlank() ? "(unknown)" : userId,
                    holdId,
                    seatId,
                    amount
            );
        } catch (Exception e) {
            log.warn("Skipping malformed booking.confirmed notification payload: {}", message, e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
