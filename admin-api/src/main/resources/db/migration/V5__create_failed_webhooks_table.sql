CREATE TABLE failed_webhooks (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    payload JSONB NOT NULL,
    error_reason TEXT,
    failed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_failed_webhooks_event_id ON failed_webhooks(event_id);

-- Update legacy deliveries that were stuck in retrying or pending
UPDATE webhook_deliveries SET status = 'FAILED' WHERE status IN ('RETRYING', 'PENDING');
