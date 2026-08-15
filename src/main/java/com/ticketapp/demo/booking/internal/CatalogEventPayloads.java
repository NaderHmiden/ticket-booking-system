package com.ticketapp.demo.booking.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Booking's local view of the JSON events Catalog publishes.
 * These records deliberately do NOT reuse Catalog's event classes — that would
 * create a cross-context dependency. {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * means Booking only pulls the fields it needs and tolerates whatever else
 * Catalog's envelope includes.
 */
final class CatalogEventPayloads {

    private CatalogEventPayloads() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TicketTypeCreated(UUID ticketTypeId, UUID eventId, int totalQuantity) {
    }
}
