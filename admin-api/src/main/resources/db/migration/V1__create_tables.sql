-- ============================
-- USERS & AUTH
-- ============================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    avatar_url TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'PLAYER',  -- ADMIN | PLAYER
    google_sub VARCHAR(255) UNIQUE,               -- Google OAuth subject ID
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================
-- DYNAMIC EVENT SCHEMAS
-- ============================
CREATE TABLE event_schemas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    schema_definition JSONB NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================
-- LIVE EVENT INSTANCES
-- ============================
CREATE TABLE live_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_id UUID REFERENCES event_schemas(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',  -- DRAFT | SCHEDULED | ACTIVE | EXPIRED
    active_from TIMESTAMPTZ NULL,
    active_until TIMESTAMPTZ NULL,
    payload JSONB NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================
-- WEBHOOK SUBSCRIPTIONS
-- ============================
CREATE TABLE webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_id UUID REFERENCES event_schemas(id),
    target_url VARCHAR(2048) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================
-- WEBHOOK DELIVERY LOG (Audit Trail)
-- ============================
CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID REFERENCES webhooks(id),
    event_id UUID REFERENCES live_events(id),
    status VARCHAR(50) NOT NULL,                  -- SUCCESS | FAILED | RETRYING
    response_code INT,
    attempt_count INT DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
