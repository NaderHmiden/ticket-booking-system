-- ÉTAPE 1: Add event_type column to catalog_event
ALTER TABLE catalog_event
ADD COLUMN event_type VARCHAR(20) NOT NULL DEFAULT 'SEATED';

-- Drop the incorrectly created ticket_type table from V1
DROP TABLE IF EXISTS ticket_type;

-- Recreate ticket_type with correct schema
CREATE TABLE ticket_type (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES catalog_event(id),
    name VARCHAR(100) NOT NULL,       -- "Standard", "VIP"
    price NUMERIC(10,2) NOT NULL,
    total_quantity INT NOT NULL,
    CONSTRAINT chk_qty CHECK (total_quantity >= 0)
);

CREATE INDEX idx_ticket_type_event_id ON ticket_type(event_id);
