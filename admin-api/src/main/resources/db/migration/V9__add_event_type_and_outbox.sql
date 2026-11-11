-- Add event_type column to webhook_deliveries for lifecycle-aware consumer idempotency
ALTER TABLE webhook_deliveries ADD COLUMN IF NOT EXISTS event_type VARCHAR(50);
UPDATE webhook_deliveries SET event_type = 'ACTIVATED' WHERE event_type IS NULL;

-- Unique constraint ensuring idempotency per webhook, event, and lifecycle event_type
CREATE UNIQUE INDEX IF NOT EXISTS uq_webhook_deliveries_idempotency 
    ON webhook_deliveries(webhook_id, event_id, event_type);

-- Transactional Outbox table for guaranteed RabbitMQ event publication
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING | PUBLISHED | FAILED
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created 
    ON outbox_events(status, created_at);

