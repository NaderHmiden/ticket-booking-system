package com.ticketapp.demo.booking;

import com.ticketapp.demo.booking.internal.SeatHoldStrategy;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SeatHoldService {

    private final SeatHoldStrategy holdStrategy;

    public SeatHoldService(SeatHoldStrategy holdStrategy) {
        this.holdStrategy = holdStrategy;
    }

    public HoldResult acquireHold(AcquireHoldRequest request) {
        return holdStrategy.acquire(request);
    }

    public boolean releaseHold(UUID seatId, UUID holdId) {
        return holdStrategy.release(seatId, holdId);
    }
}