package com.acme.coldtrace.platform.assetmanagement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.IoTDeviceCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices.IoTDeviceCommandServiceImpl;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.*;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.*;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.*;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.*;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;

class IoTDeviceManagedSourceTest {
  IoTDeviceRepository devices;
  IoTDeviceCommandServiceImpl service;
  static final String UUID_VALUE = "e846df25-9981-4911-9552-4143bf79f596";

  @BeforeEach
  void setup() {
    devices = mock(IoTDeviceRepository.class);
    var gateways = mock(GatewayRepository.class);
    var assets = mock(AssetRepository.class);
    var organizations = mock(OrganizationRepository.class);
    var sources = mock(SourceConfigurationRepository.class);
    when(organizations.existsById(7L)).thenReturn(true);
    var gateway = mock(Gateway.class);
    when(gateway.getLocationId()).thenReturn(1L);
    var asset = mock(Asset.class);
    when(asset.getLocationId()).thenReturn(1L);
    when(gateways.findByIdAndOrganizationId(anyLong(), eq(7L))).thenReturn(Optional.of(gateway));
    when(assets.findByIdAndOrganizationId(anyLong(), eq(7L))).thenReturn(Optional.of(asset));
    var device =
        new IoTDevice(
            10L,
            7L,
            3L,
            new IoTDeviceUuid(UUID_VALUE),
            "sensor",
            "TH-120",
            "temperature",
            List.of("temperature"),
            30,
            20L,
            "online",
            "calibrated",
            LocalDate.now(),
            LocalDate.now().plusYears(1));
    when(devices.lockByIdAndOrganizationId(10L, 7L)).thenReturn(Optional.of(device));
    when(devices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(sources.find(7, 10)).thenReturn(Optional.of(mock(SourceConfiguration.class)));
    service =
        new IoTDeviceCommandServiceImpl(
            devices,
            gateways,
            assets,
            organizations,
            mock(SubscriptionBillingContextFacade.class),
            sources);
  }

  private UpdateIoTDeviceCommand update(long gateway, String uuid, long asset) {
    return new UpdateIoTDeviceCommand(
        7L,
        10L,
        gateway,
        uuid,
        "sensor",
        "TH-120",
        "temperature",
        List.of("temperature"),
        30,
        asset,
        "online",
        "calibrated",
        LocalDate.now(),
        LocalDate.now().plusYears(1));
  }

  @Test
  void legacyUpdateCannotReassignAssetGatewayOrIdentity() {
    for (var command :
        List.of(
            update(3, UUID_VALUE, 21),
            update(4, UUID_VALUE, 20),
            update(3, UUID.randomUUID().toString(), 20))) {
      assertThat(service.handle(command).failure().orElseThrow())
          .isInstanceOf(IoTDeviceCommandFailure.ManagedSourceConfiguration.class);
    }
    verify(devices, never()).save(any());
  }

  @Test
  void deletionCannotRemoveAnAssociatedDevicesHistory() {
    assertThat(service.handle(new DeleteIoTDeviceCommand(7L, 10L)).failure().orElseThrow())
        .isInstanceOf(IoTDeviceCommandFailure.ManagedSourceConfiguration.class);
    verify(devices, never()).deleteById(anyLong());
  }

  @Test
  void legacyMetadataUpdateStillWorks() {
    assertThat(service.handle(update(3, UUID_VALUE, 20)).isSuccess()).isTrue();
    verify(devices).save(any());
  }
}
