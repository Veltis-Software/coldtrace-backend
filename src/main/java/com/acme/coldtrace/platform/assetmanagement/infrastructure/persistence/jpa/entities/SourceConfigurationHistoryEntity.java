package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

@Entity
@Table(
    name = "source_configuration_histories",
    indexes =
        @Index(
            name = "idx_source_profile_history",
            columnList = "organization_id,source_id,profile_version"),
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_source_device_revision",
          columnNames = {"device_id", "configuration_version"})
    })
public class SourceConfigurationHistoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "device_id", nullable = false)
  Long deviceId;

  @Column(name = "organization_id", nullable = false)
  Long organizationId;

  @Column(name = "configuration_version", nullable = false)
  Long configurationVersion;

  @Column(name = "source_id", nullable = false, length = 64)
  String sourceId;

  @Column(name = "profile_version", nullable = false)
  Integer profileVersion;

  @Column(nullable = false, columnDefinition = "TEXT")
  String snapshot;
}
