package com.ticketapp.demo.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ticket type for GENERAL_ADMISSION events, including live remaining stock when available.
 */
public record TicketTypeView(
        UUID id,
        String name,
        BigDecimal price,
        int totalQuantity,
        Integer availableQuantity
) {
}
