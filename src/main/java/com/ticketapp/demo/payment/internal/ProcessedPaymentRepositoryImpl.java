package com.ticketapp.demo.payment.internal;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC fragment for idempotent inserts (Spring Data custom impl naming).
 */
public class ProcessedPaymentRepositoryImpl implements ProcessedPaymentRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedPaymentRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean insertIfAbsent(String holdId, String status) {
        int rows = jdbcTemplate.update(
                """
                INSERT INTO processed_payments (hold_id, status, created_at)
                VALUES (?, ?, now())
                ON CONFLICT (hold_id) DO NOTHING
                """,
                holdId,
                status
        );
        return rows == 1;
    }

    @Override
    public int updateStatusIf(String holdId, String expectedStatus, String newStatus) {
        return jdbcTemplate.update(
                """
                UPDATE processed_payments
                SET status = ?
                WHERE hold_id = ? AND status = ?
                """,
                newStatus,
                holdId,
                expectedStatus
        );
    }
}
