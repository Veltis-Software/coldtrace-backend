package com.acme.coldtrace.platform.assetmanagement.domain.repositories;

import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.SourceConfiguration;
import java.util.List;
import java.util.Optional;

public interface SourceConfigurationRepository {
  Optional<SourceConfiguration> find(long organizationId, long deviceId);

  List<SourceConfiguration> history(long organizationId, long deviceId);

  List<SourceConfiguration> forGateway(long organizationId, long gatewayId);

  boolean sourceInUse(long organizationId, String sourceId, long exceptDeviceId);

  Optional<com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.SourceProfile>
      knownProfile(long organizationId, String sourceId, int profileVersion);

  /**
   * Current snapshot, immutable revision and durable event are written in the caller's transaction.
   */
  void save(SourceConfiguration configuration);
}
