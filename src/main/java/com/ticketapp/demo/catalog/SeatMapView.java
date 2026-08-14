package com.ticketapp.demo.catalog;


import java.util.List;
import java.util.UUID;

/**
 * The full seat map for one event: every seat, each with its live status.
 * What GET /api/catalog/events/{eventId}/seat-map returns.
 */
public record SeatMapView(
        UUID eventId,
        String eventName,
        List<SeatView> seats
) {
    public long availableCount() {
        return seats.stream().filter(s -> s.status() == SeatStatus.AVAILABLE).count();
    }
}
