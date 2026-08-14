package com.ticketapp.demo.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogEventController {

    private final CatalogQueryService queryService;
    private final CatalogAdminService adminService;

    public CatalogEventController(CatalogQueryService queryService, CatalogAdminService adminService) {
        this.queryService = queryService;
        this.adminService = adminService;
    }

    @GetMapping("/events")
    public List<EventSummary> listEvents() {
        return queryService.listEvents();
    }

    @GetMapping("/events/{eventId}/seat-map")
    public SeatMapView getSeatMap(@PathVariable UUID eventId) {
        return queryService.getSeatMap(eventId);
    }

    @PostMapping("/venues")
    public ResponseEntity<Map<String, UUID>> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        UUID id = adminService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("venueId", id));
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, UUID>> createEvent(@Valid @RequestBody CreateEventRequest request) {
        UUID id = adminService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("eventId", id));
    }

    @PostMapping("/events/{eventId}/seats")
    public ResponseEntity<Map<String, Object>> generateSeats(@PathVariable UUID eventId,
                                                             @Valid @RequestBody GenerateSeatsRequest request) {
        List<UUID> seatIds = adminService.generateSeats(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("seatIds", seatIds, "count", seatIds.size()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
