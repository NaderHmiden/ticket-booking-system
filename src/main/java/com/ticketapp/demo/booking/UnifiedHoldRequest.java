package com.ticketapp.demo.booking;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified request for acquiring holds, supporting both SEATED and GENERAL_ADMISSION events.
 * For SEATED: provide seatId. For GENERAL_ADMISSION: provide ticketTypeId and quantity.
 */
public record UnifiedHoldRequest(
        @NotNull UUID eventId,
        Optional<UUID> seatId,           // For SEATED events
        Optional<UUID> ticketTypeId,     // For GENERAL_ADMISSION events
        Optional<Integer> quantity,      // For GENERAL_ADMISSION events
        @NotNull UUID userId,
        java.util.List<String> passengerCins
) {
    /**
     * Validates that the request has the right fields for the event type.
     * Called by the controller after determining the event type.
     */
    public boolean isValidForEventType(com.ticketapp.demo.catalog.internal.EventType eventType) {
        return switch (eventType) {
            case SEATED -> seatId.isPresent() && ticketTypeId.isEmpty() && quantity.isEmpty();
            case GENERAL_ADMISSION, TRANSPORT_BUS -> ticketTypeId.isPresent() && quantity.isPresent() && quantity.get() > 0 && seatId.isEmpty();
        };
    }
}
