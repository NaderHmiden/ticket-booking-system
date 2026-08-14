package com.ticketapp.demo.catalog;

import java.util.UUID;

/**
 * One seat as shown on the seat map: where it is, what it costs, and its
 * current live status. This is the composed result of Catalog's write model
 * (position/price, from Postgres) and its read model (status, from Redis) —
 * assembled by {@link CatalogQueryService}, never persisted together.
 */
public record SeatView(
        UUID id,
        String section,
        String rowLabel,
        int seatNumber,
        int priceCents,
        String currency,
        SeatStatus status
) {
}
