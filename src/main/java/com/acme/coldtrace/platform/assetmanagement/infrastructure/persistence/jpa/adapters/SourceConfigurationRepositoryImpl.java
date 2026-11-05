package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.SourceConfiguration;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.SourceConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC joins the same datasource transaction as the JPA device aggregate. */
@Repository
public class SourceConfigurationRepositoryImpl implements SourceConfigurationRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json = new ObjectMapper();

  public SourceConfigurationRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<SourceConfiguration> find(long org, long device) {
    return jdbc
        .query(
            "SELECT snapshot FROM source_configurations WHERE organization_id=? AND device_id=?",
            (rs, row) -> decode(rs.getString(1)),
            org,
            device)
        .stream()
        .findFirst();
  }

  public List<SourceConfiguration> history(long org, long device) {
    return jdbc.query(
        "SELECT snapshot FROM source_configuration_histories WHERE organization_id=? AND"
            + " device_id=? ORDER BY configuration_version DESC",
        (rs, row) -> decode(rs.getString(1)),
        org,
        device);
  }

  public List<SourceConfiguration> forGateway(long org, long gateway) {
    return jdbc.query(
        "SELECT snapshot FROM source_configurations WHERE organization_id=? AND gateway_id=? ORDER"
            + " BY device_id",
        (rs, row) -> decode(rs.getString(1)),
        org,
        gateway);
  }

  public boolean sourceInUse(long org, String source, long device) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM source_configurations WHERE organization_id=? AND source_id=? AND"
                + " device_id<>?",
            Long.class,
            org,
            source,
            device)
        > 0;
  }

  public void save(SourceConfiguration configuration) {
    var snapshot = encode(configuration);
    int updated =
        jdbc.update(
            "UPDATE source_configurations SET source_id=?,configuration_version=?,snapshot=? WHERE"
                + " organization_id=? AND device_id=?",
            configuration.profile().sourceId(),
            configuration.configurationVersion(),
            snapshot,
            configuration.organizationId(),
            configuration.deviceId());
    if (updated == 0)
      jdbc.update(
          "INSERT INTO"
              + " source_configurations(device_id,organization_id,gateway_id,source_id,configuration_version,snapshot)"
              + " VALUES (?,?,?,?,?,?)",
          configuration.deviceId(),
          configuration.organizationId(),
          configuration.gatewayId(),
          configuration.profile().sourceId(),
          configuration.configurationVersion(),
          snapshot);
    jdbc.update(
        "INSERT INTO"
            + " source_configuration_histories(device_id,organization_id,configuration_version,source_id,profile_version,snapshot)"
            + " VALUES (?,?,?,?,?,?)",
        configuration.deviceId(),
        configuration.organizationId(),
        configuration.configurationVersion(),
        configuration.profile().sourceId(),
        configuration.profile().profileVersion(),
        snapshot);
    String eventId = UUID.randomUUID().toString();
    var envelope =
        Map.of(
            "eventId",
            eventId,
            "eventType",
            "source.configuration-changed",
            "version",
            1,
            "organizationId",
            configuration.organizationId(),
            "occurredAt",
            configuration.changedAt(),
            "producer",
            "asset-service",
            "payload",
            configuration);
    jdbc.update(
        "INSERT INTO asset_integration_outboxes(event_id,payload,attempts) VALUES (?,?,0)",
        eventId,
        encode(envelope));
  }

  public Optional<
          com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.SourceProfile>
      knownProfile(long org, String sourceId, int version) {
    return jdbc
        .query(
            "SELECT snapshot FROM source_configuration_histories WHERE organization_id=? AND"
                + " source_id=? AND profile_version=? ORDER BY id LIMIT 1",
            (rs, row) -> decode(rs.getString(1)).profile(),
            org,
            sourceId,
            version)
        .stream()
        .findFirst();
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot serialize source configuration", exception);
    }
  }

  private SourceConfiguration decode(String value) {
    try {
      return json.readValue(value, SourceConfiguration.class);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot read source configuration", exception);
    }
  }
}
