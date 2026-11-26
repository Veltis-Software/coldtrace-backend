-- Additive schema for the owning Asset Management database. No telemetry changes.
CREATE TABLE IF NOT EXISTS source_configurations (
  device_id BIGINT NOT NULL PRIMARY KEY,
  organization_id BIGINT NOT NULL,
  gateway_id BIGINT NOT NULL,
  source_id VARCHAR(64) NOT NULL,
  configuration_version BIGINT NOT NULL,
  snapshot TEXT NOT NULL,
  CONSTRAINT uk_source_org_identity UNIQUE (organization_id, source_id)
);
CREATE TABLE IF NOT EXISTS source_configuration_histories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  organization_id BIGINT NOT NULL,
  configuration_version BIGINT NOT NULL,
  source_id VARCHAR(64) NOT NULL,
  profile_version INT NOT NULL,
  snapshot TEXT NOT NULL,
  CONSTRAINT uk_source_device_revision UNIQUE (device_id, configuration_version),
  INDEX idx_source_profile_history (organization_id, source_id, profile_version)
);
