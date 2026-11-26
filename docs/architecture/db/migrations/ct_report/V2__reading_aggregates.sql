-- ADR-0012 · Modelo de lectura histórico · ADR-0010 · Consumidor idempotente
CREATE TABLE reading_aggregates (
  id                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organization_id    BIGINT      NOT NULL,
  asset_id           BIGINT      NOT NULL,
  hour_start         DATETIME    NOT NULL,       -- truncado a la hora (UTC)
  reading_count      INT         NOT NULL,
  out_of_range_count INT         NOT NULL,
  min_temperature    DOUBLE      NULL,
  max_temperature    DOUBLE      NULL,
  sum_temperature    DOUBLE      NULL,           -- avg = sum / count
  min_humidity       DOUBLE      NULL,
  max_humidity       DOUBLE      NULL,
  sum_humidity       DOUBLE      NULL,
  updated_at         DATETIME(6) NOT NULL,
  CONSTRAINT uk_reading_aggregates_asset_hour UNIQUE (asset_id, hour_start),
  INDEX ix_reading_aggregates_org_hour (organization_id, hour_start)
);

CREATE TABLE processed_events (
  consumer     VARCHAR(100) NOT NULL,
  event_id     CHAR(36)     NOT NULL,
  processed_at DATETIME(6)  NOT NULL,
  PRIMARY KEY (consumer, event_id)
);
