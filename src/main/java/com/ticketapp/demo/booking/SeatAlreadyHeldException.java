package com.ticketapp.demo.booking;

import com.ticketapp.demo.common.exception.DomainException;

import java.util.UUID;

public class SeatAlreadyHeldException extends DomainException {

    public SeatAlreadyHeldException(UUID seatId) {
        super("Seat " + seatId + " is no longer available");
    }

    @Override
    public String errorCode() {
        return "SEAT_UNAVAILABLE";
    }
}

