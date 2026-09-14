-- Phase 3: issued tickets, fan-to-fan resale and event waitlists.
ALTER TABLE booking_hold ADD COLUMN IF NOT EXISTS quantity INT NOT NULL DEFAULT 1;
ALTER TABLE catalog_event ADD COLUMN IF NOT EXISTS organizer_id UUID NULL REFERENCES app_user(id);
CREATE INDEX IF NOT EXISTS idx_catalog_event_organizer_id ON catalog_event(organizer_id);

CREATE TABLE ticket (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES catalog_event(id),
    owner_id UUID NOT NULL REFERENCES app_user(id),
    resource_id UUID NULL,
    tier VARCHAR(100) NOT NULL,
    price_cents INT NOT NULL,
    seat_label VARCHAR(160),
    status VARCHAR(30) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ticket_owner_id ON ticket(owner_id);
CREATE INDEX idx_ticket_event_id ON ticket(event_id);
CREATE INDEX idx_ticket_status ON ticket(status);

CREATE TABLE waitlist_entry (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES catalog_event(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    tier VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_waitlist_event_user_tier UNIQUE(event_id, user_id, tier)
);
CREATE INDEX idx_waitlist_event_tier_created ON waitlist_entry(event_id, tier, created_at);
