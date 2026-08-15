package com.ticketapp.demo.booking.internal;

import com.ticketapp.demo.booking.HoldStrategy;
import com.ticketapp.demo.catalog.internal.EventType;
import org.springframework.stereotype.Component;

/**
 * Factory to select the appropriate hold strategy based on event type.
 * SEATED events use SeatHoldStrategy, GENERAL_ADMISSION events use QuantityHoldStrategy.
 */
@Component
public class HoldStrategyFactory {

    private final SeatHoldStrategy seatHoldStrategy;
    private final QuantityHoldStrategy quantityHoldStrategy;

    public HoldStrategyFactory(SeatHoldStrategy seatHoldStrategy, QuantityHoldStrategy quantityHoldStrategy) {
        this.seatHoldStrategy = seatHoldStrategy;
        this.quantityHoldStrategy = quantityHoldStrategy;
    }

    public HoldStrategy resolve(EventType eventType) {
        return switch (eventType) {
            case SEATED -> seatHoldStrategy;
            case GENERAL_ADMISSION -> quantityHoldStrategy;
        };
    }
}
