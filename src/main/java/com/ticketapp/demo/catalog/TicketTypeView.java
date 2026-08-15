package com.ticketapp.demo.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One ticket type for a GENERAL_ADMISSION event: name, price, and total quantity.
 * Used by clients to query available ticket types for an event.
 */
public record TicketTypeView(
        UUID id,
        String name,
        BigDecimal price,
        int totalQuantity
) {
}
