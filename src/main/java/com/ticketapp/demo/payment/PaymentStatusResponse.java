package com.ticketapp.demo.payment;

/**
 * Read-only payment status for a hold. Payment still knows only holdId — never seats/events.
 */
public record PaymentStatusResponse(
        String holdId,
        String status,
        String createdAt
) {
}
