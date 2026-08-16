package com.ticketapp.demo.catalog;

import com.ticketapp.demo.catalog.internal.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CatalogQueryService {

    private static final String TICKET_AVAILABLE_KEY_PREFIX = "booking:ticket-type-available:";

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatAvailabilityReadModel availabilityReadModel;
    private final ObjectProvider<StringRedisTemplate> redisTemplate;

    public CatalogQueryService(EventRepository eventRepository,
                               VenueRepository venueRepository,
                               SeatRepository seatRepository,
                               TicketTypeRepository ticketTypeRepository,
                               SeatAvailabilityReadModel availabilityReadModel,
                               ObjectProvider<StringRedisTemplate> redisTemplate) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.seatRepository = seatRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.availabilityReadModel = availabilityReadModel;
        this.redisTemplate = redisTemplate;
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
                            event.getStatus().name(),
                            event.getEventType().name()
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
        if (!eventRepository.existsById(eventId)) {
            throw new NoSuchElementException("No event with id " + eventId);
        }

        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(tt -> new TicketTypeView(
                        tt.getId(),
                        tt.getName(),
                        tt.getPrice(),
                        tt.getTotalQuantity(),
                        readAvailableQuantity(tt.getId())
                ))
                .toList();
    }

    private Integer readAvailableQuantity(UUID ticketTypeId) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return null;
        }
        String raw = redis.opsForValue().get(TICKET_AVAILABLE_KEY_PREFIX + ticketTypeId);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
