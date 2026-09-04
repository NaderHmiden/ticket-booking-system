package com.ticketapp.demo.catalog.internal;

import com.ticketapp.demo.catalog.EventSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventSectionRepository extends JpaRepository<EventSection, UUID> {

    List<EventSection> findByEventIdOrderByCreatedAtAsc(UUID eventId);

    Optional<EventSection> findByIdAndEventId(UUID id, UUID eventId);
}
