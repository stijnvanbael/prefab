-- Prefab Streams state schema for PostgreSQL. Idempotent; safe to run on every start.

-- Keyed state, windows, join buffers and deduplication identities.
-- Windows and join buffers are rows with an expiry (expiry_key + expires_at).
CREATE TABLE IF NOT EXISTS prefab_stream_store (
    namespace  TEXT        NOT NULL,
    state_key  BYTEA       NOT NULL,
    state_value BYTEA     NOT NULL,
    version    BIGINT      NOT NULL,
    expiry_key BYTEA,
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (namespace, state_key)
);
CREATE INDEX IF NOT EXISTS prefab_stream_store_expiry
    ON prefab_stream_store (namespace, expiry_key, expires_at) WHERE expiry_key IS NOT NULL;

-- Processed input messages, for deduplication of redelivered messages.
CREATE TABLE IF NOT EXISTS prefab_stream_inbox (
    subscription TEXT        NOT NULL,
    message_id   TEXT        NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (subscription, message_id)
);
CREATE INDEX IF NOT EXISTS prefab_stream_inbox_recorded_at ON prefab_stream_inbox (recorded_at);

-- Output records waiting to be published. seq gives the publish order.
CREATE TABLE IF NOT EXISTS prefab_stream_outbox (
    seq     BIGSERIAL PRIMARY KEY,
    id      TEXT  NOT NULL UNIQUE,
    topic   TEXT  NOT NULL,
    message BYTEA NOT NULL
);
CREATE INDEX IF NOT EXISTS prefab_stream_outbox_topic ON prefab_stream_outbox (topic, seq);

-- One relay per sink topic, with expiry so another instance can take over.
CREATE TABLE IF NOT EXISTS prefab_stream_lease (
    topic      TEXT        PRIMARY KEY,
    owner      TEXT        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
