package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.booking.HoldResult;
import com.ticketapp.demo.common.event.KafkaTopics;
import com.ticketapp.demo.common.event.PaymentEventPayloads;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BookingService(BookingRepository bookingRepository,
                          KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Booking createBooking(HoldResult holdResult, UUID userId, BigDecimal amount) {
        Booking booking = new Booking(
                UUID.randomUUID(),
                holdResult.holdId().toString(),
                holdResult.eventId(),
                holdResult.seatId(),
                userId,
                amount
        );
        bookingRepository.save(booking);
        log.info("Created booking for hold {} amount={}", holdResult.holdId(), amount);
        return booking;
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findByHoldId(String holdId) {
        return bookingRepository.findByHoldId(holdId);
    }

    @Transactional
    public Booking initiatePayment(String holdId) {
        Booking booking = bookingRepository.findByHoldId(holdId)
                .orElseThrow(() -> new IllegalArgumentException("No booking for hold " + holdId));

        if (!"HELD".equals(booking.getStatus())) {
            log.info("Booking for hold {} already in status {}, skipping payment initiation (idempotence)",
                    holdId, booking.getStatus());
            return booking;
        }

        int updated = bookingRepository.markPaymentRequested(holdId);
        if (updated == 0) {
            return bookingRepository.findByHoldId(holdId).orElse(booking);
        }

        Booking updatedBooking = bookingRepository.findByHoldId(holdId).orElse(booking);

        try {
            PaymentEventPayloads.PaymentRequested event = new PaymentEventPayloads.PaymentRequested(
                    holdId,
                    updatedBooking.getAmount(),
                    Instant.now()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.PAYMENT_REQUESTS, holdId, json);
            log.info("Published PaymentRequested for hold {}", holdId);
        } catch (Exception e) {
            log.error("Failed to publish PaymentRequested for hold {}: {}", holdId, e.getMessage());
        }

        return updatedBooking;
    }
}
