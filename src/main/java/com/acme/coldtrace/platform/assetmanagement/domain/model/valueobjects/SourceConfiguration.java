package com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects;

/** Immutable snapshot. Historical rows retain their original device, gateway and asset. */
public record SourceConfiguration(
    long organizationId,
    long deviceId,
    String deviceUuid,
    long gatewayId,
    long assetId,
    long configurationVersion,
    SourceProfile profile,
    long changedBy,
    String changedAt,
    String reason) {}
