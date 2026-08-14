/**
 * Booking's internal implementation: the Seat/Booking aggregates, JPA
 * repositories, the Redis hold client. Off-limits to every other bounded
 * context — enforced by {@code com.ticketbooking.architecture.ModuleBoundaryTest}.
 */
package com.ticketapp.demo.booking.internal;
