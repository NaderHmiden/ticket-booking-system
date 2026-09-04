package com.ticketapp.demo.catalog;

import com.ticketapp.demo.catalog.internal.EventListing;
import com.ticketapp.demo.catalog.internal.EventRepository;
import com.ticketapp.demo.catalog.internal.EventSectionRepository;
import com.ticketapp.demo.catalog.internal.EventType;
import com.ticketapp.demo.catalog.internal.Seat;
import com.ticketapp.demo.catalog.internal.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EventSectionService {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final int MAX_SEATS_PER_SECTION = 100_000;

    private final EventRepository eventRepository;
    private final EventSectionRepository sectionRepository;
    private final SeatRepository seatRepository;

    public EventSectionService(EventRepository eventRepository,
                               EventSectionRepository sectionRepository,
                               SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.sectionRepository = sectionRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public EventSection saveSectionAndGenerateSeats(UUID eventId, CreateSectionRequest request) {
        EventListing event = getSeatedEvent(eventId);
        validateGeometry(request);
        ensureDoesNotOverlap(event.getId(), request);

        EventSection section = sectionRepository.save(new EventSection(UUID.randomUUID(), eventId, request));
        int seatCount = seatCount(request);
        List<Seat> seats = new ArrayList<>(seatCount);
        for (int row = request.startRow(); row <= request.endRow(); row++) {
            for (int col = request.startCol(); col <= request.endCol(); col++) {
                seats.add(new Seat(UUID.randomUUID(), eventId, section.getName(), String.valueOf(row), col,
                        section.getPriceCents(), DEFAULT_CURRENCY, section.getId()));
            }
        }
        seatRepository.saveAll(seats);
        return section;
    }

    @Transactional(readOnly = true)
    public List<EventSection> getSectionsForEvent(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NoSuchElementException("No event with id " + eventId);
        }
        return sectionRepository.findByEventIdOrderByCreatedAtAsc(eventId);
    }

    @Transactional
    public void deleteSectionAndSeats(UUID eventId, UUID sectionId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NoSuchElementException("No event with id " + eventId);
        }
        EventSection section = sectionRepository.findByIdAndEventId(sectionId, eventId)
                .orElseThrow(() -> new NoSuchElementException("No section with id " + sectionId + " for event " + eventId));
        seatRepository.deleteByEventSectionId(section.getId());
        sectionRepository.delete(section);
    }

    private EventListing getSeatedEvent(UUID eventId) {
        EventListing event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("No event with id " + eventId));
        if (event.getEventType() != EventType.SEATED) {
            throw new IllegalArgumentException("Sections can only be created for SEATED events");
        }
        return event;
    }

    private void ensureDoesNotOverlap(UUID eventId, CreateSectionRequest candidate) {
        boolean overlaps = sectionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .anyMatch(existing -> rectanglesOverlap(existing, candidate));
        if (overlaps) {
            throw new IllegalArgumentException("Section geometry overlaps an existing section");
        }
    }

    private static boolean rectanglesOverlap(EventSection existing, CreateSectionRequest candidate) {
        return existing.getStartRow() <= candidate.endRow()
                && candidate.startRow() <= existing.getEndRow()
                && existing.getStartCol() <= candidate.endCol()
                && candidate.startCol() <= existing.getEndCol();
    }

    private static void validateGeometry(CreateSectionRequest request) {
        if (request.startRow() > request.endRow()) {
            throw new IllegalArgumentException("startRow must be less than or equal to endRow");
        }
        if (request.startCol() > request.endCol()) {
            throw new IllegalArgumentException("startCol must be less than or equal to endCol");
        }
        seatCount(request);
    }

    private static int seatCount(CreateSectionRequest request) {
        long rows = (long) request.endRow() - request.startRow() + 1;
        long columns = (long) request.endCol() - request.startCol() + 1;
        long count = rows * columns;
        if (count > MAX_SEATS_PER_SECTION) {
            throw new IllegalArgumentException("A section cannot contain more than " + MAX_SEATS_PER_SECTION + " seats");
        }
        return (int) count;
    }
}
