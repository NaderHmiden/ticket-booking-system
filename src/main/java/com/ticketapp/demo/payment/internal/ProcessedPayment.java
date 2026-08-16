package com.ticketapp.demo.payment.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_payments")
public class ProcessedPayment {

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @Column(name = "hold_id", nullable = false)
    private String holdId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProcessedPayment() {
    }

    public ProcessedPayment(String holdId, String status) {
        this.holdId = holdId;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getHoldId() {
        return holdId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
