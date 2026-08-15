package com.ticketapp.demo.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * Request to acquire a hold for GENERAL_ADMISSION tickets.
 * Specifies quantity instead of a single seatId.
 */
public record QuantityAcquireHoldRequest(
        @NotNull UUID eventId,
        @NotNull UUID ticketTypeId,
        @Positive int quantity,
        @NotNull UUID userId
) { }
