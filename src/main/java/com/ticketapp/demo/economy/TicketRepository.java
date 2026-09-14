package com.ticketapp.demo.economy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<Ticket> findByEventIdInOrderByCreatedAtAsc(List<UUID> eventIds);
    long countByEventIdIn(List<UUID> eventIds);
    long countByEventIdInAndStatus(List<UUID> eventIds, TicketStatus status);
}
