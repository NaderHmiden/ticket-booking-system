package com.ticketapp.demo.economy;

import com.ticketapp.demo.catalog.internal.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/analytics/events")
public class AnalyticsController {
    private final EventRepository events; private final TicketRepository tickets; private final WaitlistRepository waitlist;
    public AnalyticsController(EventRepository events, TicketRepository tickets, WaitlistRepository waitlist) { this.events = events; this.tickets = tickets; this.waitlist = waitlist; }
    @GetMapping("/{eventId}")
    public AnalyticsResponse dashboard(@PathVariable String eventId, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean organizer = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER"));
        if (!admin && !organizer) throw new AccessDeniedException("Organizer analytics require ADMIN or ORGANIZER access");
        UUID userId = (UUID) authentication.getPrincipal();
        List<UUID> ids = "all".equalsIgnoreCase(eventId)
                ? (admin ? events.findAll() : events.findByOrganizerId(userId)).stream().map(e -> e.getId()).toList()
                : List.of(parseEvent(eventId, admin, userId));
        if (ids.isEmpty()) return empty();
        List<Ticket> sold = tickets.findByEventIdInOrderByCreatedAtAsc(ids);
        List<WaitlistEntry> waits = waitlist.findByEventIdInOrderByCreatedAtAsc(ids);
        long revenue = sold.stream().mapToLong(Ticket::getPriceCents).sum();
        Map<String, Integer> tiers = new TreeMap<>(); sold.forEach(t -> tiers.merge(t.getTier(), 1, Integer::sum));
        Map<String, long[]> velocity = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:00").withZone(ZoneOffset.UTC);
        sold.forEach(t -> velocity.computeIfAbsent(fmt.format(t.getCreatedAt()), k -> new long[2])[0] += t.getPriceCents());
        waits.forEach(w -> velocity.computeIfAbsent(fmt.format(w.getCreatedAt()), k -> new long[2])[1]++);
        return new AnalyticsResponse(new Kpis(revenue, sold.size(), waits.size(), sold.stream().filter(t -> t.getStatus() == TicketStatus.RESALE_PENDING).count()),
                velocity.entrySet().stream().map(e -> new RevenuePoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList(),
                tiers.entrySet().stream().map(e -> new TierPoint(e.getKey(), e.getValue())).toList());
    }
    private UUID parseEvent(String value, boolean admin, UUID userId) { try { UUID id = UUID.fromString(value); var event = events.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found")); if (!admin && !userId.equals(event.getOrganizerId())) throw new AccessDeniedException("You do not organize this event"); return id; } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId must be a UUID or all"); } }
    private static AnalyticsResponse empty() { return new AnalyticsResponse(new Kpis(0, 0, 0, 0), List.of(), List.of()); }
    public record AnalyticsResponse(Kpis kpis, List<RevenuePoint> revenueVelocity, List<TierPoint> salesByTier) { }
    public record Kpis(long totalRevenueCents, long ticketsSold, long waitlistDemand, long resaleActivity) { }
    public record RevenuePoint(String time, long revenue, long waitlist) { }
    public record TierPoint(String name, int value) { }
}
