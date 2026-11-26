-- ADR-0005 / ADR-0011 · Clave del gateway y versión de rangos
ALTER TABLE gateways ADD COLUMN api_key_hash VARCHAR(100) NULL;           -- BCrypt de X-Gateway-Key
ALTER TABLE asset_settings ADD COLUMN settings_version BIGINT NOT NULL DEFAULT 1;  -- se incrementa en cada cambio de rango
