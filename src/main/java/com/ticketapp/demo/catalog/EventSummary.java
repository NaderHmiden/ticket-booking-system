package com.ticketapp.demo.catalog;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the event browse listing.
 */
public record EventSummary(
        UUID id,
        String name,
        String category,
        String venueName,
        String city,
        Instant startsAt,
        String status,
        String eventType
) {
}
