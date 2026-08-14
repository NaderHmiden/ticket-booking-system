/**
 * Booking bounded context — the centerpiece of this project. Owns seat
 * inventory state and the hold/reserve/confirm/release lifecycle that
 * guarantees a seat is never sold twice, even under a flash-sale thundering
 * herd. See README.md for the concurrency strategy (Redis atomic SET NX,
 * not a long-held DB transaction) before touching this context.
 *
 * <p>Only classes in this package (not {@code .internal}) are the public
 * surface other contexts may depend on.
 */
package com.ticketapp.demo.booking;
