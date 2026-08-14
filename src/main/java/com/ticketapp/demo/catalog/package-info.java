/**
 * Catalog bounded context. Owns events, venues, seat maps, and pricing, and
 * exposes the denormalized, cache-friendly read model used for browsing
 * (kept eventually consistent with Booking's write model via domain events —
 * see the ADR in README.md, "Read model consistency").
 *
 * <p>Only classes in this package (not {@code .internal}) are the public
 * surface other contexts may depend on.
 */
package com.ticketapp.demo.catalog;
