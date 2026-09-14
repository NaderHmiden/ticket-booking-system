package com.ticketapp.demo.economy;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {
    boolean existsByEventIdAndUserIdAndTier(UUID eventId, UUID userId, String tier);
    long countByEventIdAndTier(UUID eventId, String tier);
    List<WaitlistEntry> findByEventIdInOrderByCreatedAtAsc(List<UUID> eventIds);
    long countByEventIdIn(List<UUID> eventIds);
}
