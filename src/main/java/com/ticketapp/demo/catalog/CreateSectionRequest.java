package com.ticketapp.demo.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Geometry and presentation details for one rectangular venue section. */
public record CreateSectionRequest(
        @NotBlank @Size(max = 255) String name,
        @PositiveOrZero int startRow,
        @PositiveOrZero int endRow,
        @PositiveOrZero int startCol,
        @PositiveOrZero int endCol,
        @NotBlank @Size(max = 20) String color,
        @PositiveOrZero int priceCents
) { }
