-- Booking hold lifecycle + payment stub idempotence tables

CREATE TABLE booking_hold (
    id UUID PRIMARY KEY,
    hold_id VARCHAR(36) NOT NULL UNIQUE,
    event_id UUID NOT NULL REFERENCES catalog_event(id),
    resource_id UUID NOT NULL,
    user_id UUID NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD',
    amount NUMERIC(10, 2) NOT NULL,
    payment_requested BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_booking_hold_hold_id ON booking_hold(hold_id);
CREATE INDEX idx_booking_hold_status ON booking_hold(status);

CREATE TABLE processed_payments (
    hold_id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_payments_created_at ON processed_payments(created_at);
