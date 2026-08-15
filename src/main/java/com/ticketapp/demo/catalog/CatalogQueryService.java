package com.ticketapp.demo.catalog;


import com.ticketapp.demo.catalog.internal.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CatalogQueryService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatAvailabilityReadModel availabilityReadModel;

    public CatalogQueryService(EventRepository eventRepository,
                               VenueRepository venueRepository,
                               SeatRepository seatRepository,
                               TicketTypeRepository ticketTypeRepository,
                               SeatAvailabilityReadModel availabilityReadModel) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.seatRepository = seatRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.availabilityReadModel = availabilityReadModel;
    }

    public List<EventSummary> listEvents() {
        Map<UUID, Venue> venuesById = venueRepository.findAll().stream()
                .collect(Collectors.toMap(Venue::getId, v -> v));

        return eventRepository.findAllByOrderByStartsAtAsc().stream()
                .map(event -> {
                    var venue = venuesById.get(event.getVenueId());
                    return new EventSummary(
                            event.getId(),
                            event.getName(),
                            event.getCategory(),
                            venue != null ? venue.getName() : "Unknown venue",
                            venue != null ? venue.getCity() : "Unknown city",
                            event.getStartsAt(),
                            event.getStatus().name()
                    );
                })
                .toList();
    }

    public SeatMapView getSeatMap(UUID eventId) {
        EventListing event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("No event with id " + eventId));

        List<Seat> seats = seatRepository.findByEventIdOrderBySectionAscRowLabelAscSeatNumberAsc(eventId);
        Map<UUID, SeatStatus> overrides = availabilityReadModel.getStatuses(
                seats.stream().map(Seat::getId).toList());

        List<SeatView> seatViews = seats.stream()
                .map(seat -> new SeatView(
                        seat.getId(),
                        seat.getSection(),
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getPriceCents(),
                        seat.getCurrency(),
                        overrides.getOrDefault(seat.getId(), SeatStatus.AVAILABLE)
                ))
                .toList();

        return new SeatMapView(event.getId(), event.getName(), seatViews);
    }

    public List<TicketTypeView> getTicketTypes(UUID eventId) {
        EventListing event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("No event with id " + eventId));

        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(eventId);
        return ticketTypes.stream()
                .map(tt -> new TicketTypeView(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getTotalQuantity()
                ))
                .toList();
    }
}
