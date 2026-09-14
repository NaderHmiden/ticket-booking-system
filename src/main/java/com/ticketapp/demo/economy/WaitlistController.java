package com.ticketapp.demo.economy;

import com.ticketapp.demo.catalog.internal.EventRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class WaitlistController {
    private final WaitlistRepository waitlist; private final EventRepository events;
    public WaitlistController(WaitlistRepository waitlist, EventRepository events) { this.waitlist = waitlist; this.events = events; }
    @PostMapping("/{eventId}/waitlist")
    public ResponseEntity<Map<String, Object>> join(@PathVariable UUID eventId, @Valid @RequestBody JoinWaitlistRequest request, Authentication authentication) {
        if (!events.existsById(eventId)) throw new IllegalArgumentException("Event not found");
        UUID userId = TicketController.currentUser(authentication); String tier = request.tier().trim();
        if (waitlist.existsByEventIdAndUserIdAndTier(eventId, userId, tier)) throw new IllegalStateException("Already on this waitlist");
        waitlist.save(new WaitlistEntry(UUID.randomUUID(), eventId, userId, tier));
        long position = waitlist.countByEventIdAndTier(eventId, tier);
        return ResponseEntity.ok(Map.of("message", "Successfully joined the waitlist", "positionInQueue", position));
    }
    public record JoinWaitlistRequest(@NotBlank String tier) { }
}
