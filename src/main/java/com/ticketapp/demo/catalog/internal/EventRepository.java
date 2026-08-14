package com.ticketapp.demo.catalog.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventListing, UUID> {

    List<EventListing> findAllByOrderByStartsAtAsc();
}
