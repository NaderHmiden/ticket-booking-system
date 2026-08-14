package com.ticketapp.demo.common.event;



/**
 * Kafka topic names that cross a bounded-context boundary. Centralized here
 * (the shared kernel) so a typo in a topic name is a compile error, not a
 * silent "consumer never receives anything" bug discovered at 2am during a
 * flash sale.
 *
 * <p>Deliberately just names — not the event payload types. Catalog (the
 * consumer, built in Step 2) deserializes these into its own local DTOs
 * matching the expected JSON shape rather than importing Booking's event
 * classes directly; Booking (the producer, built in Step 3) is expected to
 * publish events shaped to match. This keeps the two contexts coupled only
 * through a wire format, never through shared Java types — so
 * ModuleBoundaryTest's "no context depends on another context's public
 * surface" rule stays true even for the event-driven path.
 */
public final class KafkaTopics {

    /** Published by Booking when a seat hold is acquired. Payload includes at least: seatId, expiresAt. */
    public static final String BOOKING_SEAT_HELD = "booking.seat-held";

    /** Published by Booking when a hold is released. Payload: seatId. */
    public static final String BOOKING_SEAT_RELEASED = "booking.seat-released";

    /** Published by Booking when a booking is confirmed. Payload: seatId. */
    public static final String BOOKING_CONFIRMED = "booking.confirmed";

    private KafkaTopics() {
    }
}
