/**
 * Catalog's internal implementation: JPA entities, repositories, the read-model
 * projection logic. Off-limits to every other bounded context — enforced by
 * {@code com.ticketbooking.architecture.ModuleBoundaryTest}. If Booking needs
 * something from here, it belongs on the public {@code com.ticketbooking.catalog}
 * surface (an application service or DTO) or as a Kafka event, not a direct import.
 */
package com.ticketapp.demo.catalog.internal;
