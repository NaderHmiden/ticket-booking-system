package com.ticketapp.demo.booking.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.demo.booking.HoldStrategy;
import com.ticketapp.demo.catalog.internal.EventListing;
import com.ticketapp.demo.catalog.internal.EventRepository;
import com.ticketapp.demo.catalog.internal.EventType;
import com.ticketapp.demo.economy.TicketService;
import com.ticketapp.demo.common.event.KafkaTopics;
import com.ticketapp.demo.common.event.PaymentEventPayloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes payment results and applies conditional booking state transitions.
 */
@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final BookingRepository bookingRepository;
    private final HoldStrategyFactory holdStrategyFactory;
    private final EventRepository eventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TicketService ticketService;

    public PaymentResultListener(
            BookingRepository bookingRepository,
            HoldStrategyFactory holdStrategyFactory,
            EventRepository eventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            TicketService ticketService) {
        this.bookingRepository = bookingRepository;
        this.holdStrategyFactory = holdStrategyFactory;
        this.eventRepository = eventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.ticketService = ticketService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_RESULTS, groupId = "booking-payment-result-processor")
    @Transactional
    public void onPaymentResult(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("succeededAt")) {
                PaymentEventPayloads.PaymentSucceeded succeeded =
                        objectMapper.treeToValue(node, PaymentEventPayloads.PaymentSucceeded.class);
                onSucceeded(succeeded);
            } else if (node.has("failedAt")) {
                PaymentEventPayloads.PaymentFailed failed =
                        objectMapper.treeToValue(node, PaymentEventPayloads.PaymentFailed.class);
                onFailed(failed);
            } else {
                log.warn("Unrecognized payment.results payload: {}", message);
            }
        } catch (Exception e) {
            log.warn("Skipping malformed payment.results message: {}", message, e);
        }
    }

    private void onSucceeded(PaymentEventPayloads.PaymentSucceeded event) throws Exception {
        String holdId = event.holdId();
        int updated = bookingRepository.confirmIfPending(holdId);
        if (updated == 0) {
            log.info("duplicate or already resolved — confirmIfPending affected 0 rows for hold {}", holdId);
            publishExpiredHoldAnomalyIfNeeded(holdId);
            return;
        }

        confirmHold(holdId);
    }

    private void onFailed(PaymentEventPayloads.PaymentFailed event) {
        String holdId = event.holdId();
        int updated = bookingRepository.releaseIfPending(holdId);
        if (updated == 0) {
            log.info("duplicate or already resolved — releaseIfPending affected 0 rows for hold {}", holdId);
            return;
        }

        releaseHold(holdId);
    }

    private void confirmHold(String holdId) {
        Booking booking = bookingRepository.findByHoldId(holdId)
                .orElseThrow(() -> new IllegalStateException("Booking missing after confirm for hold " + holdId));
        HoldStrategy strategy = resolveStrategy(booking);
        strategy.confirm(booking.getResourceId(), UUID.fromString(holdId));
        ticketService.issueFor(booking);

        // Notify catalog (seat → BOOKED) and notification stub
        try {
            kafkaTemplate.send(
                    KafkaTopics.BOOKING_CONFIRMED,
                    booking.getResourceId().toString(),
                    objectMapper.writeValueAsString(Map.of(
                            "seatId", booking.getResourceId().toString(),
                            "holdId", holdId,
                            "eventId", booking.getEventId().toString(),
                            "userId", booking.getUserId() != null ? booking.getUserId().toString() : "",
                            "amount", booking.getAmount()
                    ))
            );
        } catch (Exception e) {
            log.warn("Failed to publish booking.confirmed for hold {}: {}", holdId, e.getMessage());
        }
        log.info("Confirmed hold {} via HoldStrategy", holdId);
    }

    private void releaseHold(String holdId) {
        Booking booking = bookingRepository.findByHoldId(holdId)
                .orElseThrow(() -> new IllegalStateException("Booking missing after release for hold " + holdId));
        HoldStrategy strategy = resolveStrategy(booking);
        strategy.release(booking.getResourceId(), UUID.fromString(holdId));
        log.info("Released hold {} via HoldStrategy", holdId);
    }

    private HoldStrategy resolveStrategy(Booking booking) {
        EventListing event = eventRepository.findById(booking.getEventId())
                .orElseThrow(() -> new IllegalStateException("No event for booking " + booking.getHoldId()));
        EventType eventType = event.getEventType();
        return holdStrategyFactory.resolve(eventType);
    }

    /**
     * Late PaymentSucceeded after hold expiry/release — audit only, no refund yet.
     */
    private void publishExpiredHoldAnomalyIfNeeded(String holdId) throws Exception {
        Booking booking = bookingRepository.findByHoldId(holdId).orElse(null);
        if (booking == null) {
            return;
        }
        // Only emit anomaly when payment succeeded against a non-pending booking
        // (typically already RELEASED after expiry).
        if ("PAYMENT_PENDING".equals(booking.getStatus()) || "CONFIRMED".equals(booking.getStatus())) {
            return;
        }

        BigDecimal amount = booking.getAmount();
        PaymentEventPayloads.PaymentReceivedForExpiredHold anomaly =
                new PaymentEventPayloads.PaymentReceivedForExpiredHold(holdId, amount);
        kafkaTemplate.send(KafkaTopics.PAYMENT_ANOMALIES, holdId, objectMapper.writeValueAsString(anomaly));
        log.info("Published PaymentReceivedForExpiredHold for hold {} (status={})", holdId, booking.getStatus());
    }
}
