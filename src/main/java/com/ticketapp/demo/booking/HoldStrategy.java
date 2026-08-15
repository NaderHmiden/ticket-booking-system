package com.ticketapp.demo.booking;

import java.util.UUID;

/**
 * Strategy interface for hold mechanisms. Implementations vary by event type:
 * - SeatHoldStrategy: SEATED events, holds individual seats via Redis SET NX.
 * - QuantityHoldStrategy: GENERAL_ADMISSION events, decrements shared stock via Lua script.
 */
public interface HoldStrategy {

    /**
     * Attempts to acquire a hold. Returns HoldResult if successful, throws exception if not.
     */
    HoldResult acquire(AcquireHoldRequest request);

    /**
     * Releases a hold by holdId. Returns true if released, false if already released/expired.
     */
    boolean release(UUID seatId, UUID holdId);

    /**
     * Confirms a hold permanently, completing the reservation.
     */
    boolean confirm(UUID seatId, UUID holdId);
}
