ALTER TABLE catalog_event
ADD COLUMN arrival_venue_id UUID NULL,
ADD COLUMN arrives_at TIMESTAMP WITH TIME ZONE NULL,
ADD COLUMN transport_company VARCHAR(255) NULL;


ALTER TABLE booking_hold
ADD COLUMN passenger_cins VARCHAR(255) NULL;
