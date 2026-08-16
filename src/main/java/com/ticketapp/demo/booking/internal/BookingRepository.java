package com.ticketapp.demo.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Booking entities with conditional update methods for idempotence.
 */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByHoldId(String holdId);

    /**
     * Confirms a booking if it's in PAYMENT_PENDING status.
     * Returns the number of rows updated (0 or 1).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Booking b SET b.status = 'CONFIRMED' WHERE b.holdId = :holdId AND b.status = 'PAYMENT_PENDING'")
    int confirmIfPending(@Param("holdId") String holdId);

    /**
     * Releases a booking if it's in PAYMENT_PENDING status.
     * Returns the number of rows updated (0 or 1).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Booking b SET b.status = 'RELEASED' WHERE b.holdId = :holdId AND b.status = 'PAYMENT_PENDING'")
    int releaseIfPending(@Param("holdId") String holdId);

    /**
     * Sets payment_requested and transitions to PAYMENT_PENDING only if currently HELD.
     * Returns the number of rows updated (0 or 1).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Booking b SET b.status = 'PAYMENT_PENDING', b.paymentRequested = true "
            + "WHERE b.holdId = :holdId AND b.status = 'HELD'")
    int markPaymentRequested(@Param("holdId") String holdId);
}
