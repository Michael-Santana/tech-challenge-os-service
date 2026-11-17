CREATE TABLE order_outbox (
    event_id VARCHAR(36) PRIMARY KEY,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);

CREATE INDEX idx_order_outbox_pending ON order_outbox (created_at, event_id)
    WHERE published_at IS NULL;
