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

    @GetMapping("/events/{eventId}/ticket-types")
    public List<TicketTypeView> getTicketTypes(@PathVariable UUID eventId) {
        return queryService.getTicketTypes(eventId);
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

    @PostMapping("/events/{eventId}/ticket-types")
    public ResponseEntity<Map<String, UUID>> createTicketType(@PathVariable UUID eventId,
                                                              @Valid @RequestBody CreateTicketTypeRequest request) {
        UUID id = adminService.createTicketType(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ticketTypeId", id));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }
}
