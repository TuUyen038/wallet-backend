CREATE TABLE idempotency_keys (
    key         TEXT PRIMARY KEY,
    status      TEXT NOT NULL DEFAULT 'PROCESSING',
    response    JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);