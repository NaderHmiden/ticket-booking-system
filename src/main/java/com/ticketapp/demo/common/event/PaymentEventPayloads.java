package com.ticketapp.demo.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Shared wire-format DTOs for payment events that cross the Booking ↔ Payment
 * boundary. Payment must never learn about seats, tickets, or event types —
 * only holdId and amount.
 */
public final class PaymentEventPayloads {

    private PaymentEventPayloads() {
    }

    public record PaymentRequested(
            String holdId,
            BigDecimal amount,
            Instant requestedAt
    ) {
    }

    public record PaymentSucceeded(
            String holdId,
            Instant succeededAt
    ) {
    }

    public record PaymentFailed(
            String holdId,
            String reason,
            Instant failedAt
    ) {
    }

    /**
     * Published by Booking when PaymentSucceeded arrives for an already-expired/released hold.
     */
    public record PaymentReceivedForExpiredHold(
            String holdId,
            BigDecimal amount
    ) {
    }
}
