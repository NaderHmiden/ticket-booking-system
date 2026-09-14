package com.ticketapp.demo.economy;

import com.ticketapp.demo.catalog.internal.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketRepository tickets; private final TicketService service; private final EventRepository events;
    public TicketController(TicketRepository tickets, TicketService service, EventRepository events) { this.tickets = tickets; this.service = service; this.events = events; }

    @GetMapping("/me")
    public List<TicketView> mine(Authentication authentication) {
        UUID userId = currentUser(authentication);
        return tickets.findByOwnerIdOrderByCreatedAtDesc(userId).stream().map(ticket -> {
            var event = events.findById(ticket.getEventId()).orElseThrow(() -> new IllegalStateException("Event not found"));
            return new TicketView(ticket.getId(), ticket.getEventId(), event.getName(), service.venueName(ticket.getEventId()), event.getStartsAt(), ticket.getTier(), ticket.getPriceCents(), ticket.getStatus(), ticket.getSeatLabel());
        }).toList();
    }

    @PostMapping("/{ticketId}/resell")
    public ResponseEntity<Map<String, Object>> resell(@PathVariable UUID ticketId, Authentication authentication) {
        Ticket ticket = service.listForResale(ticketId, currentUser(authentication));
        return ResponseEntity.ok(Map.of("message", "Ticket successfully listed for resale", "ticketId", ticket.getId(), "status", ticket.getStatus()));
    }

    static UUID currentUser(Authentication authentication) { return (UUID) authentication.getPrincipal(); }
    public record TicketView(UUID id, UUID eventId, String eventName, String venueName, java.time.Instant date, String tier, int priceCents, TicketStatus status, String seatLabel) { }
}
