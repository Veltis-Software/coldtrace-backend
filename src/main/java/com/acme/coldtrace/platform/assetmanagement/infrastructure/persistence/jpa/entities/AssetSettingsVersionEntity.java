package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

@Entity
@Table(name="asset_settings_versions")
public class AssetSettingsVersionEntity {
  @Id @Column(name="asset_id") Long assetId;
  @Column(name="settings_version",nullable=false) long settingsVersion;
}
