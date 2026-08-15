package com.ticketapp.demo.booking;

import java.time.Instant;
import java.util.UUID;

public record HoldResult(
        UUID holdId,
        UUID seatId,
        UUID eventId,
        Instant expiresAt
) { }