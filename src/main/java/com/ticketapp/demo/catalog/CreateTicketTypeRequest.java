package com.ticketapp.demo.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateTicketTypeRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        @Positive int totalQuantity
) {
}
