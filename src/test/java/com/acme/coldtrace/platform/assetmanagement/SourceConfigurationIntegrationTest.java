package com.acme.coldtrace.platform.assetmanagement;

import static org.assertj.core.api.Assertions.*;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.SourceConfigurationException;
import com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices.SourceConfigurationService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.SourceProfile;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.IoTDeviceRepository;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs against the configured MySQL test database, including real JPA/JDBC transactions and locks.
 */
@SpringBootTest
class SourceConfigurationIntegrationTest {
  private static final AtomicLong IDS = new AtomicLong(700_000_000L);
  @Autowired SourceConfigurationService service;
  @Autowired IoTDeviceRepository devices;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;
  long org, device, asset, gateway;

  @BeforeEach
  void fixture() {
    org = IDS.getAndAdd(10);
    device = org + 1;
    asset = org + 2;
    gateway = org + 3;
    jdbc.update(
        "INSERT INTO gateways(id,organization_id,location_id,uuid,name,network,status) VALUES"
            + " (?,?,?,?,?,?,?)",
        gateway,
        org,
        1,
        UUID.randomUUID().toString(),
        "Gateway almacén norte",
        "ethernet",
        "online");
    jdbc.update(
        "INSERT INTO assets(id,organization_id,location_id,uuid,type,name,capacity,status) VALUES"
            + " (?,?,?,?,?,?,?,?)",
        asset,
        org,
        1,
        UUID.randomUUID().toString(),
        "cold-room",
        "Cámara de lácteos",
        100.0,
        "active");
    insertDevice(device);
  }

  private void insertDevice(long id) {
    jdbc.update(
        "INSERT INTO"
            + " iot_devices(id,organization_id,gateway_id,uuid,device_type,model,measurement_type,reading_frequency_seconds,status,calibration_status,last_calibration_date,next_calibration_date)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_DATE,CURRENT_DATE)",
        id,
        org,
        gateway,
        UUID.randomUUID().toString(),
        "sensor",
        "TH-120",
        "temperature",
        30,
        "online",
        "calibrated");
    jdbc.update(
        "INSERT INTO iot_device_measurement_parameters(iot_device_id,measurement_parameter) VALUES"
            + " (?,?)",
        id,
        "temperature");
  }

  @AfterEach
  void cleanup() {
    jdbc.update(
        "DELETE FROM asset_integration_outboxes WHERE JSON_EXTRACT(payload,'$.organizationId')=?",
        org);
    jdbc.update("DELETE FROM source_configuration_histories WHERE organization_id=?", org);
    jdbc.update("DELETE FROM source_configurations WHERE organization_id=?", org);
    jdbc.update(
        "DELETE FROM iot_device_measurement_parameters WHERE iot_device_id IN (SELECT id FROM"
            + " iot_devices WHERE organization_id=?)",
        org);
    jdbc.update("DELETE FROM iot_devices WHERE organization_id=?", org);
    jdbc.update("DELETE FROM assets WHERE organization_id=?", org);
    jdbc.update("DELETE FROM gateways WHERE organization_id=?", org);
  }

  private SourceProfile profile(String source, String transport, int version, String unit) {
    return new SourceProfile(
        source,
        transport,
        version,
        1,
        Map.of("temperature", "temp", "humidity", "rh", "recordedAt", "timestamp"),
        unit,
        "%RH");
  }

  @Test
  void associationAndReplacementKeepImmutableSnapshotsAndDurableEvents() {
    var first =
        service.configure(
            org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación de sonda");
    var second =
        service.configure(
            org,
            device,
            asset,
            1,
            profile("probe-b", "http", 1, "F"),
            13,
            "Sustitución por calibración");
    assertThat(second.configurationVersion()).isEqualTo(2);
    assertThat(service.current(org, device)).isEqualTo(second);
    assertThat(service.history(org, device)).containsExactly(second, first);
    assertThat(devices.findByIdAndOrganizationId(device, org).orElseThrow().getAssetId())
        .isEqualTo(asset);
    assertThat(service.forGateway(org, gateway)).containsExactly(second);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset_integration_outboxes WHERE"
                    + " JSON_EXTRACT(payload,'$.organizationId')=? AND"
                    + " JSON_UNQUOTE(JSON_EXTRACT(payload,'$.eventType'))='source.configuration-changed'",
                Long.class,
                org))
        .isEqualTo(2);
    assertThat(first.profile().sourceId()).isEqualTo("probe-a");
    assertThat(second.deviceUuid()).isEqualTo(first.deviceUuid());
  }

  @Test
  void rejectsStaleEditsWithoutWritingAnything() {
    var first =
        service.configure(
            org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación");
    assertThatThrownBy(
            () ->
                service.configure(
                    org, device, asset, 0, profile("probe-b", "http", 1, "F"), 13, "Reemplazo"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("reload");
    assertThat(service.history(org, device)).containsExactly(first);
  }

  @Test
  void rejectsCrossOrganizationAndIncompatibleLocation() {
    assertThatThrownBy(
            () ->
                service.configure(
                    org + 1000,
                    device,
                    asset,
                    0,
                    profile("probe-a", "mqtt", 1, "C"),
                    12,
                    "Instalación"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("Device not found");
    jdbc.update("UPDATE assets SET location_id=2 WHERE id=?", asset);
    assertThatThrownBy(
            () ->
                service.configure(
                    org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("location");
    assertThat(service.history(org, device)).isEmpty();
  }

  @Test
  void oneSourceCannotBelongToTwoDevices() {
    insertDevice(device + 4);
    service.configure(org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación");
    assertThatThrownBy(
            () ->
                service.configure(
                    org,
                    device + 4,
                    asset,
                    0,
                    profile("probe-a", "mqtt", 1, "C"),
                    12,
                    "Instalación"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("already associated");
    assertThat(service.history(org, device + 4)).isEmpty();
  }

  @Test
  void initialConfigurationCannotMoveAnAlreadyAssignedLegacyDevice() {
    jdbc.update("UPDATE iot_devices SET asset_id=? WHERE id=?", asset, device);
    assertThatThrownBy(() -> service.configure(org, device, asset + 1, 0,
        profile("probe-a", "mqtt", 1, "C"), 12, "Instalación"))
        .isInstanceOf(SourceConfigurationException.class).hasMessageContaining("retain");
    assertThat(service.history(org, device)).isEmpty();
    assertThat(devices.findByIdAndOrganizationId(device, org).orElseThrow().getAssetId()).isEqualTo(asset);
  }

  @Test
  void sameSourceRequiresIncreasingProfileVersionAndReplacementRetainsAsset() {
    service.configure(org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación");
    assertThatThrownBy(
            () ->
                service.configure(
                    org, device, asset, 1, profile("probe-a", "http", 1, "F"), 12, "Actualización"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("cannot be redefined");
    assertThatThrownBy(
            () ->
                service.configure(
                    org,
                    device,
                    asset + 1,
                    1,
                    profile("probe-b", "http", 1, "F"),
                    12,
                    "Actualización"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("retain");
    var revised =
        service.configure(
            org,
            device,
            asset,
            1,
            profile("probe-a", "http", 2, "F"),
            12,
            "Actualización de protocolo");
    assertThat(revised.profile().profileVersion()).isEqualTo(2);
  }

  @Test
  void rollbackIncludesDeviceCurrentHistoryAndOutbox() {
    var tx = new TransactionTemplate(transactions);
    assertThatThrownBy(
            () ->
                tx.execute(
                    status -> {
                      service.configure(
                          org,
                          device,
                          asset,
                          0,
                          profile("probe-a", "mqtt", 1, "C"),
                          12,
                          "Instalación");
                      throw new IllegalStateException("simulated commit failure");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(service.history(org, device)).isEmpty();
    assertThat(devices.findByIdAndOrganizationId(device, org).orElseThrow().getAssetId()).isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM source_configurations WHERE organization_id=?",
                Long.class,
                org))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset_integration_outboxes WHERE"
                    + " JSON_EXTRACT(payload,'$.organizationId')=?",
                Long.class,
                org))
        .isZero();
  }

  @Test
  void replacedSourceCannotRedefineItsHistoricalProfileVersion() {
    service.configure(org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación");
    service.configure(org, device, asset, 1, profile("probe-b", "http", 1, "F"), 12, "Reemplazo");
    assertThatThrownBy(
            () ->
                service.configure(
                    org, device, asset, 2, profile("probe-a", "http", 1, "F"), 12, "Reinstalación"))
        .isInstanceOf(SourceConfigurationException.class)
        .hasMessageContaining("cannot be redefined");
    assertThat(service.history(org, device)).hasSize(2);
  }

  @Test
  void concurrentInitialAssociationsHaveOneWinner() throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var ready = new CountDownLatch(2);
      var start = new CountDownLatch(1);
      Callable<Boolean> change =
          () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
              service.configure(
                  org, device, asset, 0, profile("probe-a", "mqtt", 1, "C"), 12, "Instalación");
              return true;
            } catch (SourceConfigurationException conflict) {
              assertThat(conflict.kind()).isEqualTo(SourceConfigurationException.Kind.CONFLICT);
              return false;
            }
          };
      var a = executor.submit(change);
      var b = executor.submit(change);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
      assertThat(service.history(org, device)).hasSize(1);
    }
  }

  @Test
  void profileRejectsAmbiguousMappingsAndUnsupportedContracts() {
    assertThatThrownBy(
            () ->
                new SourceProfile(
                    "probe-a",
                    "http",
                    1,
                    1,
                    Map.of("temperature", "x", "humidity", "x", "recordedAt", "at"),
                    "C",
                    "%RH"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new SourceProfile(
                    "probe-a",
                    "http",
                    1,
                    2,
                    Map.of("temperature", "x", "humidity", "y", "recordedAt", "at"),
                    "C",
                    "%RH"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
