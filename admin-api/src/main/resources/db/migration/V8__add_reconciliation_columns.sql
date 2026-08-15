-- Add columns needed for webhook reconciliation retries
ALTER TABLE webhook_deliveries ADD COLUMN IF NOT EXISTS target_url VARCHAR(2048);
ALTER TABLE webhook_deliveries ADD COLUMN IF NOT EXISTS payload JSONB;
ALTER TABLE webhook_deliveries ADD COLUMN IF NOT EXISTS failure_reason TEXT;

-- Create index for the reconciliation query
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_status_last_attempt
    ON webhook_deliveries(status, last_attempt_at);
