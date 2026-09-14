package com.ticketapp.demo.economy;

import com.ticketapp.demo.booking.internal.Booking;
import com.ticketapp.demo.catalog.internal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public TicketService(TicketRepository ticketRepository, EventRepository eventRepository, VenueRepository venueRepository, SeatRepository seatRepository, TicketTypeRepository ticketTypeRepository) {
        this.ticketRepository = ticketRepository; this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.seatRepository = seatRepository; this.ticketTypeRepository = ticketTypeRepository;
    }

    /** Called after the conditional booking confirmation; therefore it runs once per paid hold. */
    @Transactional
    public void issueFor(Booking booking) {
        if (booking.getUserId() == null) return;
        if (seatRepository.existsById(booking.getResourceId())) {
            Seat seat = seatRepository.findById(booking.getResourceId()).orElseThrow();
            ticketRepository.save(new Ticket(UUID.randomUUID(), booking.getEventId(), booking.getUserId(), seat.getId(),
                    seat.getSection(), seat.getPriceCents(), "Section " + seat.getSection() + " - Row " + seat.getRowLabel() + " - Seat " + seat.getSeatNumber()));
            return;
        }
        TicketType type = ticketTypeRepository.findById(booking.getResourceId()).orElseThrow(() -> new IllegalStateException("Ticket type missing for booking"));
        int individualPrice = type.getPrice().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        for (int i = 0; i < booking.getQuantity(); i++) {
            ticketRepository.save(new Ticket(UUID.randomUUID(), booking.getEventId(), booking.getUserId(), type.getId(), type.getName(), individualPrice, null));
        }
    }

    @Transactional
    public Ticket listForResale(UUID ticketId, UUID userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        if (!ticket.getOwnerId().equals(userId)) throw new SecurityException("You do not own this ticket");
        ticket.listForResale();
        return ticket;
    }

    public String eventName(UUID eventId) { return eventRepository.findById(eventId).map(EventListing::getName).orElse("Unknown event"); }
    public String venueName(UUID eventId) {
        return eventRepository.findById(eventId).map(EventListing::getVenueId)
                .flatMap(venueRepository::findById).map(Venue::getName).orElse("Unknown venue");
    }
}
