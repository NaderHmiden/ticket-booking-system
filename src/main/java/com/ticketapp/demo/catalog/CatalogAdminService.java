package com.ticketapp.demo.catalog;

import com.ticketapp.demo.catalog.internal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CatalogAdminService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final CatalogEventPublisher eventPublisher;

    public CatalogAdminService(VenueRepository venueRepository,
                               EventRepository eventRepository,
                               SeatRepository seatRepository,
                               TicketTypeRepository ticketTypeRepository,
                               CatalogEventPublisher eventPublisher) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventPublisher = eventPublisher;
    }

    public UUID createVenue(CreateVenueRequest request) {
        Venue venue = new Venue(UUID.randomUUID(), request.name(), request.city(), request.timezone());
        venueRepository.save(venue);
        return venue.getId();
    }

    public UUID createEvent(CreateEventRequest request) {
        if (!venueRepository.existsById(request.venueId())) {
            throw new NoSuchElementException("No venue with id " + request.venueId());
        }
        EventType eventType = parseEventType(request.eventType());
        EventListing event = new EventListing(
                UUID.randomUUID(), request.venueId(), request.name(), request.category(),
                request.startsAt(), eventType);
        eventRepository.save(event);
        return event.getId();
    }

    @Transactional
    public List<UUID> generateSeats(UUID eventId, GenerateSeatsRequest request) {
        EventListing event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("No event with id " + eventId));
        if (event.getEventType() != EventType.SEATED) {
            throw new IllegalArgumentException("Seats can only be generated for SEATED events");
        }
        String currency = request.currency() == null || request.currency().isBlank() ? "USD" : request.currency();

        List<Seat> seats = new ArrayList<>(request.count());
        for (int i = 0; i < request.count(); i++) {
            int seatNumber = request.startingSeatNumber() + i;
            seats.add(new Seat(
                    UUID.randomUUID(), eventId, request.section(), request.rowLabel(),
                    seatNumber, request.priceCents(), currency));
        }
        seatRepository.saveAll(seats);
        return seats.stream().map(Seat::getId).toList();
    }

    @Transactional
    public UUID createTicketType(UUID eventId, CreateTicketTypeRequest request) {
        EventListing event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("No event with id " + eventId));
        if (event.getEventType() != EventType.GENERAL_ADMISSION) {
            throw new IllegalArgumentException("Ticket types require a GENERAL_ADMISSION event");
        }

        TicketType ticketType = new TicketType(
                UUID.randomUUID(), eventId, request.name(), request.price(), request.totalQuantity());
        ticketTypeRepository.save(ticketType);
        eventPublisher.publish(new TicketTypeCreated(ticketType.getId(), eventId, request.totalQuantity()));
        return ticketType.getId();
    }

    private static EventType parseEventType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EventType.SEATED;
        }
        try {
            return EventType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("eventType must be SEATED or GENERAL_ADMISSION");
        }
    }
}
