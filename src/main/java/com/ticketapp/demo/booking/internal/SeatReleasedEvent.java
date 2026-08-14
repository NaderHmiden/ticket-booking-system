package com.ticketapp.demo.booking.internal;


import com.ticketapp.demo.common.event.DomainEvent;
import com.ticketapp.demo.common.event.KafkaTopics;

import java.util.UUID;

/**
 * Published when a seat hold is released — explicitly (user cancels
 * checkout) in this step; payment-failure-triggered release is Step 4.
 * Same aggregateId-by-seatId reasoning as {@link SeatHeldEvent}.
 */
public final class SeatReleasedEvent extends DomainEvent {

    private final UUID seatId;

    public SeatReleasedEvent(UUID seatId) {
        super(seatId.toString());
        this.seatId = seatId;
    }

    public UUID getSeatId() {
        return seatId;
    }

    @Override
    public String topic() {
        return KafkaTopics.BOOKING_SEAT_RELEASED;
    }
}
