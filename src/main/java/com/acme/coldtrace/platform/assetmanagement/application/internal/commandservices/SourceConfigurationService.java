package com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices;

import static com.acme.coldtrace.platform.assetmanagement.application.commandservices.SourceConfigurationException.Kind.*;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.SourceConfigurationException;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.*;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SourceConfigurationService {
  private final IoTDeviceRepository devices;
  private final GatewayRepository gateways;
  private final AssetRepository assets;
  private final SourceConfigurationRepository configurations;

  public SourceConfigurationService(
      IoTDeviceRepository devices,
      GatewayRepository gateways,
      AssetRepository assets,
      SourceConfigurationRepository configurations) {
    this.devices = devices;
    this.gateways = gateways;
    this.assets = assets;
    this.configurations = configurations;
  }

  @Transactional
  public SourceConfiguration configure(
      long organizationId,
      long deviceId,
      long assetId,
      long expectedVersion,
      SourceProfile profile,
      long actorId,
      String reason) {
    if (profile == null
        || expectedVersion < 0
        || actorId <= 0
        || reason == null
        || reason.isBlank()
        || reason.strip().length() > 500) {
      throw new SourceConfigurationException(
          INVALID, "A valid profile, version and reason are required");
    }
    // The same lock is used by legacy device updates/deletes, including initial association.
    var device =
        devices
            .lockByIdAndOrganizationId(deviceId, organizationId)
            .orElseThrow(() -> new SourceConfigurationException(NOT_FOUND, "Device not found"));
    var previous = configurations.find(organizationId, deviceId);
    if (device.getAssetId() != null && device.getAssetId() != assetId) {
      throw new SourceConfigurationException(CONFLICT, "Source configuration must retain the device's monitored asset");
    }
    long version = previous.map(SourceConfiguration::configurationVersion).orElse(0L);
    if (expectedVersion != version) {
      throw new SourceConfigurationException(
          CONFLICT, "Configuration changed; reload before saving");
    }
    if (previous.isPresent() && previous.get().assetId() != assetId) {
      throw new SourceConfigurationException(
          CONFLICT, "Source replacement must retain the monitored asset");
    }
    if (configurations.sourceInUse(organizationId, profile.sourceId(), deviceId)) {
      throw new SourceConfigurationException(
          CONFLICT, "Source is already associated with another device");
    }
    if (configurations
        .knownProfile(organizationId, profile.sourceId(), profile.profileVersion())
        .filter(known -> !known.equals(profile))
        .isPresent()) {
      throw new SourceConfigurationException(
          CONFLICT, "An existing profile version cannot be redefined");
    }
    if (previous.isPresent()
        && previous.get().profile().sourceId().equals(profile.sourceId())
        && !previous.get().profile().equals(profile)
        && profile.profileVersion() <= previous.get().profile().profileVersion()) {
      throw new SourceConfigurationException(
          CONFLICT, "A changed source profile requires a higher profile version");
    }
    var gateway =
        gateways
            .findByIdAndOrganizationId(device.getGatewayId(), organizationId)
            .orElseThrow(() -> new SourceConfigurationException(NOT_FOUND, "Gateway not found"));
    var asset =
        assets
            .findByIdAndOrganizationId(assetId, organizationId)
            .orElseThrow(() -> new SourceConfigurationException(NOT_FOUND, "Asset not found"));
    if (!asset.getLocationId().equals(gateway.getLocationId())) {
      throw new SourceConfigurationException(
          INVALID, "Asset and gateway must share the same location");
    }
    try {
      UUID.fromString(device.getUuid());
    } catch (IllegalArgumentException invalid) {
      throw new SourceConfigurationException(
          INVALID, "Device requires a UUID compatible with Edge");
    }
    device.associateSourceAsset(assetId);
    devices.save(device);
    var configuration =
        new SourceConfiguration(
            organizationId,
            deviceId,
            device.getUuid(),
            device.getGatewayId(),
            assetId,
            version + 1,
            profile,
            actorId,
            Instant.now().toString(),
            reason.strip());
    configurations.save(configuration);
    return configuration;
  }

  public SourceConfiguration current(long organizationId, long deviceId) {
    requireDevice(organizationId, deviceId);
    return configurations
        .find(organizationId, deviceId)
        .orElseThrow(() -> new SourceConfigurationException(NOT_FOUND, "Source is not configured"));
  }

  public List<SourceConfiguration> history(long organizationId, long deviceId) {
    requireDevice(organizationId, deviceId);
    return configurations.history(organizationId, deviceId);
  }

  public List<SourceConfiguration> forGateway(long organizationId, long gatewayId) {
    if (gateways.findByIdAndOrganizationId(gatewayId, organizationId).isEmpty()) {
      throw new SourceConfigurationException(NOT_FOUND, "Gateway not found");
    }
    return configurations.forGateway(organizationId, gatewayId);
  }

  private void requireDevice(long organizationId, long deviceId) {
    if (devices.findByIdAndOrganizationId(deviceId, organizationId).isEmpty()) {
      throw new SourceConfigurationException(NOT_FOUND, "Device not found");
    }
  }
}
