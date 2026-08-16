package com.ticketapp.demo.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingStatusResponse(
        UUID id,
        String holdId,
        UUID eventId,
        UUID userId,
        String status,
        BigDecimal amount,
        boolean paymentRequested,
        Instant createdAt,
        Instant updatedAt
) {
}
