package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.common.event.DomainEvent;
import com.ticketapp.demo.common.event.KafkaTopics;

import java.util.UUID;

/**
 * Published when payment succeeds and the hold is confirmed.
 * Catalog projects this to mark seats BOOKED; Notification stubs an email.
 */
public final class BookingConfirmedEvent extends DomainEvent {

    private final UUID seatId;
    private final String holdId;

    public BookingConfirmedEvent(UUID seatId, String holdId) {
        super(seatId.toString());
        this.seatId = seatId;
        this.holdId = holdId;
    }

    public UUID getSeatId() {
        return seatId;
    }

    public String getHoldId() {
        return holdId;
    }

    @Override
    public String topic() {
        return KafkaTopics.BOOKING_CONFIRMED;
    }
}
