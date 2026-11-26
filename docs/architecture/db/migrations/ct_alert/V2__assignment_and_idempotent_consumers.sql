-- ADR-0014 · Reconocer y asignar · ADR-0010 · Consumidor idempotente
ALTER TABLE incidents
  ADD COLUMN assigned_to_user_id BIGINT      NULL AFTER acknowledged_by,
  ADD COLUMN assigned_at         DATETIME(6) NULL AFTER assigned_to_user_id;

CREATE INDEX ix_incidents_asset_ack ON incidents (asset_id, acknowledged_at);

CREATE TABLE processed_events (
  consumer     VARCHAR(100) NOT NULL,
  event_id     CHAR(36)     NOT NULL,
  processed_at DATETIME(6)  NOT NULL,
  PRIMARY KEY (consumer, event_id)
);
