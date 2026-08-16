package com.ticketapp.demo.booking;

import com.ticketapp.demo.booking.internal.Booking;
import com.ticketapp.demo.booking.internal.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{holdId}")
    public ResponseEntity<BookingStatusResponse> getStatus(@PathVariable String holdId) {
        Booking booking = bookingService.findByHoldId(holdId)
                .orElseThrow(() -> new IllegalArgumentException("No booking for hold " + holdId));
        return ResponseEntity.ok(toResponse(booking));
    }

    /**
     * Initiates asynchronous payment for a held booking. Returns 202 with current state.
     */
    @PostMapping("/{holdId}/confirm")
    public ResponseEntity<BookingStatusResponse> confirm(@PathVariable String holdId) {
        Booking booking = bookingService.initiatePayment(holdId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(booking));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    private static BookingStatusResponse toResponse(Booking booking) {
        return new BookingStatusResponse(
                booking.getId(),
                booking.getHoldId(),
                booking.getEventId(),
                booking.getUserId(),
                booking.getStatus(),
                booking.getAmount(),
                booking.isPaymentRequested(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
