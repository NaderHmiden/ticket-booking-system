package com.ticketapp.demo.booking;

import com.ticketapp.demo.booking.internal.HoldStrategyFactory;
import com.ticketapp.demo.booking.internal.QuantityHoldStrategy;
import com.ticketapp.demo.catalog.internal.EventRepository;
import com.ticketapp.demo.catalog.internal.EventType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;
    private final HoldStrategyFactory holdStrategyFactory;
    private final QuantityHoldStrategy quantityHoldStrategy;
    private final EventRepository eventRepository;

    public SeatHoldController(SeatHoldService seatHoldService,
                             HoldStrategyFactory holdStrategyFactory,
                             QuantityHoldStrategy quantityHoldStrategy,
                             EventRepository eventRepository) {
        this.seatHoldService = seatHoldService;
        this.holdStrategyFactory = holdStrategyFactory;
        this.quantityHoldStrategy = quantityHoldStrategy;
        this.eventRepository = eventRepository;
    }

    /**
     * Unified endpoint to acquire holds for both SEATED and GENERAL_ADMISSION events.
     * For backward compatibility, also supports the original AcquireHoldRequest.
     */
    @PostMapping("/holds")
    public ResponseEntity<HoldResult> acquireHold(@Valid @RequestBody UnifiedHoldRequest request) {
        // Fetch event to determine type
        var event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new NoSuchElementException("No event with id " + request.eventId()));

        EventType eventType = event.getEventType();

        if (!request.isValidForEventType(eventType)) {
            return ResponseEntity.badRequest().build();
        }

        HoldResult result = switch (eventType) {
            case SEATED -> {
                UUID seatId = request.seatId()
                        .orElseThrow(() -> new IllegalArgumentException("seatId required for SEATED events"));
                AcquireHoldRequest seatRequest = new AcquireHoldRequest(request.eventId(), seatId, request.userId());
                yield seatHoldService.acquireHold(seatRequest);
            }
            case GENERAL_ADMISSION -> {
                UUID ticketTypeId = request.ticketTypeId()
                        .orElseThrow(() -> new IllegalArgumentException("ticketTypeId required for GENERAL_ADMISSION events"));
                int quantity = request.quantity()
                        .orElseThrow(() -> new IllegalArgumentException("quantity required for GENERAL_ADMISSION events"));
                yield quantityHoldStrategy.acquireQuantity(ticketTypeId, quantity, request.eventId(), request.userId());
            }
        };

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Original endpoint for backward compatibility (supports AcquireHoldRequest directly).
     * This allows existing clients to continue working without changes.
     */
    @PostMapping("/holds/seat")
    public ResponseEntity<HoldResult> acquireHoldLegacy(@Valid @RequestBody AcquireHoldRequest request) {
        HoldResult result = seatHoldService.acquireHold(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/holds/{holdId}")
    public ResponseEntity<Void> releaseHold(@PathVariable UUID holdId, @RequestParam UUID seatId) {
        boolean released = seatHoldService.releaseHold(seatId, holdId);
        return released ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(SeatAlreadyHeldException.class)
    public ResponseEntity<Map<String, String>> handleSeatUnavailable(SeatAlreadyHeldException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.errorCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientInventory(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "INSUFFICIENT_INVENTORY", "message", e.getMessage()));
    }
}
