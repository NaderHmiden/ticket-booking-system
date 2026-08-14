package com.ticketapp.demo.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GenerateSeatsRequest(
        @NotBlank String section, @NotBlank String rowLabel,
        @Positive int startingSeatNumber, @Positive int count,
        @Positive int priceCents, String currency
) { }