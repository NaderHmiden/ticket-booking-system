package com.ticketapp.demo.booking;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AcquireHoldRequest(
        @NotNull UUID eventId,
        @NotNull UUID seatId,
        @NotNull UUID userId
) { }
