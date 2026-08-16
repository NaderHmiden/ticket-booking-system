package com.ticketapp.demo.booking;

import com.ticketapp.demo.booking.internal.Booking;
import com.ticketapp.demo.booking.internal.BookingService;
import com.ticketapp.demo.booking.internal.QuantityHoldStrategy;
import com.ticketapp.demo.catalog.internal.EventRepository;
import com.ticketapp.demo.catalog.internal.EventType;
import com.ticketapp.demo.catalog.internal.SeatRepository;
import com.ticketapp.demo.catalog.internal.TicketType;
import com.ticketapp.demo.catalog.internal.TicketTypeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/booking")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;
    private final QuantityHoldStrategy quantityHoldStrategy;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final BookingService bookingService;

    public SeatHoldController(SeatHoldService seatHoldService,
                              QuantityHoldStrategy quantityHoldStrategy,
                              EventRepository eventRepository,
                              SeatRepository seatRepository,
                              TicketTypeRepository ticketTypeRepository,
                              BookingService bookingService) {
        this.seatHoldService = seatHoldService;
        this.quantityHoldStrategy = quantityHoldStrategy;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.bookingService = bookingService;
    }

    @PostMapping("/holds")
    public ResponseEntity<HoldResult> acquireHold(@Valid @RequestBody UnifiedHoldRequest request) {
        var event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new NoSuchElementException("No event with id " + request.eventId()));

        EventType eventType = event.getEventType();
        if (!request.isValidForEventType(eventType)) {
            return ResponseEntity.badRequest().build();
        }

        HoldResult result;
        BigDecimal amount;

        switch (eventType) {
            case SEATED -> {
                UUID seatId = request.seatId().orElseThrow();
                AcquireHoldRequest seatRequest = new AcquireHoldRequest(request.eventId(), seatId, request.userId());
                result = seatHoldService.acquireHold(seatRequest);
                var seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new NoSuchElementException("No seat with id " + seatId));
                amount = BigDecimal.valueOf(seat.getPriceCents(), 2);
            }
            case GENERAL_ADMISSION -> {
                UUID ticketTypeId = request.ticketTypeId().orElseThrow();
                int quantity = request.quantity().orElseThrow();
                result = quantityHoldStrategy.acquireQuantity(
                        ticketTypeId, quantity, request.eventId(), request.userId());
                TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                        .orElseThrow(() -> new NoSuchElementException("No ticket type with id " + ticketTypeId));
                amount = ticketType.getPrice()
                        .multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            default -> throw new IllegalStateException("Unsupported event type " + eventType);
        }

        bookingService.createBooking(result, request.userId(), amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/holds/seat")
    public ResponseEntity<HoldResult> acquireHoldLegacy(@Valid @RequestBody AcquireHoldRequest request) {
        HoldResult result = seatHoldService.acquireHold(request);
        var seat = seatRepository.findById(request.seatId())
                .orElseThrow(() -> new NoSuchElementException("No seat with id " + request.seatId()));
        BigDecimal amount = BigDecimal.valueOf(seat.getPriceCents(), 2);
        bookingService.createBooking(result, request.userId(), amount);
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

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }
}
