package com.ticketapp.demo.common.exception;

/**
 * Base type for exceptions that represent a legitimate domain-rule violation
 * (as opposed to a bug or infrastructure failure). Each bounded context defines
 * its own specific subclasses (e.g. {@code SeatAlreadyHeldException} in Booking);
 * this base type exists only so cross-cutting concerns (a REST @ControllerAdvice
 * in the app package, metrics tagging) can handle "domain rejection" uniformly
 * without depending on any single context's exception classes.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * A short, stable machine-readable code (e.g. "SEAT_UNAVAILABLE") for clients
     * and metrics — deliberately not the exception's class name, which is free to
     * change without breaking API consumers or dashboards.
     */
    public abstract String errorCode();
}
