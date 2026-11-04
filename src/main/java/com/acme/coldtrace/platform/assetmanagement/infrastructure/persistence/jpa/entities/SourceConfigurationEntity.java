package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

@Entity
@Table(
    name = "source_configurations",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_source_org_identity",
          columnNames = {"organization_id", "source_id"})
    })
public class SourceConfigurationEntity {
  @Id
  @Column(name = "device_id")
  Long deviceId;

  @Column(name = "organization_id", nullable = false)
  Long organizationId;

  @Column(name = "gateway_id", nullable = false)
  Long gatewayId;

  @Column(name = "source_id", nullable = false, length = 64)
  String sourceId;

  @Column(name = "configuration_version", nullable = false)
  Long configurationVersion;

  @Column(nullable = false, columnDefinition = "TEXT")
  String snapshot;
}
