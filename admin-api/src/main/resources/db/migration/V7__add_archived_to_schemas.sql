-- Add archived flag to event_schemas for soft delete support
ALTER TABLE event_schemas ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_event_schemas_archived ON event_schemas(archived);
