package com.ticketapp.demo.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateEventRequest(
        @NotNull UUID venueId,
        @NotBlank String name,
        @NotBlank String category,
        @NotNull Instant startsAt
) {
}
