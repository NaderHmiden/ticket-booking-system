CREATE TABLE event_sections (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES catalog_event(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    start_row INT NOT NULL,
    end_row INT NOT NULL,
    start_col INT NOT NULL,
    end_col INT NOT NULL,
    color VARCHAR(20) NOT NULL,
    price_cents INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_event_sections_rows CHECK (start_row <= end_row),
    CONSTRAINT chk_event_sections_columns CHECK (start_col <= end_col),
    CONSTRAINT chk_event_sections_price CHECK (price_cents >= 0)
);

CREATE INDEX idx_event_sections_event_id ON event_sections(event_id);

-- A section owns the seats generated from its geometry. This makes section
-- deletion precise even when sections share a display name.
ALTER TABLE catalog_seat
    ADD COLUMN event_section_id UUID REFERENCES event_sections(id) ON DELETE CASCADE;

CREATE INDEX idx_catalog_seat_event_section_id ON catalog_seat(event_section_id);
