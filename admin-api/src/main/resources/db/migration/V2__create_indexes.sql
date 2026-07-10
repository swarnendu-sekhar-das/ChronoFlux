-- ============================
-- PERFORMANCE INDEXES
-- ============================
CREATE INDEX idx_live_events_status ON live_events(status, active_from);
CREATE INDEX idx_live_events_payload_gin ON live_events USING GIN (payload);
CREATE INDEX idx_users_google_sub ON users(google_sub);
