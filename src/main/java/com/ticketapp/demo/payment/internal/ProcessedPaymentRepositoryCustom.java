package com.ticketapp.demo.payment.internal;

/**
 * Custom idempotent insert / conditional status update for processed payments.
 */
public interface ProcessedPaymentRepositoryCustom {

    /**
     * INSERT ... ON CONFLICT DO NOTHING. Returns {@code true} if this call inserted the row.
     */
    boolean insertIfAbsent(String holdId, String status);

    /**
     * Conditional status update. Returns number of rows affected.
     */
    int updateStatusIf(String holdId, String expectedStatus, String newStatus);
}
