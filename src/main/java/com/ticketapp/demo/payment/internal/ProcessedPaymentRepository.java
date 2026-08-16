package com.ticketapp.demo.payment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for payment-processing idempotence rows.
 */
public interface ProcessedPaymentRepository
        extends JpaRepository<ProcessedPayment, String>, ProcessedPaymentRepositoryCustom {
}
