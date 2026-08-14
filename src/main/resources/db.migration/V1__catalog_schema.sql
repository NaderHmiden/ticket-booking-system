-- Catalog bounded context: write-model schema.
--
-- This is the SOURCE OF TRUTH for the seat map's static structure (which
-- seats exist, where, at what price) — not for live availability. Whether a
-- seat is currently available/held/booked lives in Catalog's Redis read
-- model (see SeatAvailabilityReadModel), kept in sync via Kafka events
-- published by the Booking context. Deliberately no "status" column here:
-- putting it here would tempt someone into querying/joining across it from
-- Booking, recreating the shared-table coupling this project's whole
-- module-boundary story exists to avoid.

CREATE TABLE catalog_venue (
                               id          UUID PRIMARY KEY,
                               name        VARCHAR(200) NOT NULL,
                               city        VARCHAR(120) NOT NULL,
                               timezone    VARCHAR(64)  NOT NULL,
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE catalog_event (
                               id          UUID PRIMARY KEY,
                               venue_id    UUID NOT NULL REFERENCES catalog_venue(id),
                               name        VARCHAR(200) NOT NULL,
                               category    VARCHAR(40)  NOT NULL,
                               starts_at   TIMESTAMPTZ  NOT NULL,
                               status      VARCHAR(20)  NOT NULL DEFAULT 'ON_SALE',
                               created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_catalog_event_venue_id ON catalog_event(venue_id);
CREATE INDEX idx_catalog_event_starts_at ON catalog_event(starts_at);

CREATE TABLE catalog_seat (
                              id           UUID PRIMARY KEY,
                              event_id     UUID NOT NULL REFERENCES catalog_event(id),
                              section      VARCHAR(40) NOT NULL,
                              row_label    VARCHAR(10) NOT NULL,
                              seat_number  INT         NOT NULL,
                              price_cents  INT         NOT NULL,
                              currency     VARCHAR(3)  NOT NULL DEFAULT 'USD',
    -- One seat per (event, section, row, number): this is what makes the
    -- flash-sale seed idempotent to re-run and prevents accidentally
    -- double-defining the same physical seat for an event.
                              CONSTRAINT uq_catalog_seat_position UNIQUE (event_id, section, row_label, seat_number)
);

CREATE INDEX idx_catalog_seat_event_id ON catalog_seat(event_id);