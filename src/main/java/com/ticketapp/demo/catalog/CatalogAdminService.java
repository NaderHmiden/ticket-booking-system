package com.ticketapp.demo.catalog;

import com.ticketapp.demo.catalog.internal.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CatalogAdminService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public CatalogAdminService(VenueRepository venueRepository,
                               EventRepository eventRepository,
                               SeatRepository seatRepository) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
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
        EventListing event = new EventListing(
                UUID.randomUUID(), request.venueId(), request.name(), request.category(), request.startsAt());
        eventRepository.save(event);
        return event.getId();
    }

    @Transactional
    public List<UUID> generateSeats(UUID eventId, GenerateSeatsRequest request) {
        if (!eventRepository.existsById(eventId)) {
            throw new NoSuchElementException("No event with id " + eventId);
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
}
