package com.acme.coldtrace.platform.assetmanagement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices.SourceConfigurationService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.*;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.SourceConfigurationsController;
import com.acme.coldtrace.platform.iam.interfaces.acl.SessionContextFacade;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SourceConfigurationsControllerTest {
  SourceConfigurationService service;
  SessionContextFacade sessions;
  MockMvc mvc;
  final UsernamePasswordAuthenticationToken principal =
      new UsernamePasswordAuthenticationToken("operator@example.test", "unused", List.of());
  final String path = "/api/v1/organizations/7/iot-devices/10/source-configuration";
  final String request =
      """
{"assetId":20,"expectedVersion":0,"sourceId":"probe-a","transport":"http",
 "profileVersion":1,"schemaVersion":1,"temperatureUnit":"F","humidityUnit":"%RH",
 "fields":{"temperature":"temp","humidity":"rh","recordedAt":"timestamp"},"reason":"Sustitución por calibración"}
""";

  @BeforeEach
  void setup() {
    service = mock(SourceConfigurationService.class);
    sessions = mock(SessionContextFacade.class);
    mvc =
        MockMvcBuilders.standaloneSetup(new SourceConfigurationsController(service, sessions))
            .build();
  }

  void permissions(long org, String... permissions) {
    when(sessions.resolve(principal.getName()))
        .thenReturn(
            Optional.of(
                new SessionContextFacade.SessionContext(
                    12L, org, "Operador", List.of(permissions))));
  }

  @Test
  void rejectsAnonymousUnknownAndForeignTenantsBeforeAccessingAssets() throws Exception {
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(get(path).principal(principal)).andExpect(status().isUnauthorized());
    permissions(8, "assets:manage");
    mvc.perform(get(path).principal(principal)).andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void versionsRejectCoercionAndOverflow() throws Exception {
    permissions(7, "assets:manage");
    for (String invalid : List.of("1.5", "\"1\"", "true", "2147483648")) {
      mvc.perform(
              put(path)
                  .principal(principal)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      request.replace("\"profileVersion\":1", "\"profileVersion\":" + invalid)))
          .andExpect(status().isBadRequest());
    }
    for (String invalid : List.of("0.5", "\"0\"", "false", "9223372036854775808")) {
      mvc.perform(
              put(path)
                  .principal(principal)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      request.replace("\"expectedVersion\":0", "\"expectedVersion\":" + invalid)))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(service);
  }

  @Test
  void readerCanQueryButCannotWrite() throws Exception {
    permissions(7, "assets:view");
    mvc.perform(get(path + "/history").principal(principal)).andExpect(status().isOk());
    mvc.perform(
            put(path).principal(principal).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isForbidden());
    verify(service).history(7, 10);
    verifyNoMoreInteractions(service);
  }

  @Test
  void managerUsesIamActorAndExportsExactEdgeDocument() throws Exception {
    permissions(7, "assets:manage");
    var profile =
        new SourceProfile(
            "probe-a",
            "http",
            1,
            1,
            Map.of("temperature", "temp", "humidity", "rh", "recordedAt", "timestamp"),
            "F",
            "%RH");
    var configuration =
        new SourceConfiguration(
            7,
            10,
            "e846df25-9981-4911-9552-4143bf79f596",
            3,
            20,
            1,
            profile,
            12,
            "2026-11-03T12:00:00Z",
            "Sustitución por calibración");
    when(service.configure(7, 10, 20, 0, profile, 12, "Sustitución por calibración"))
        .thenReturn(configuration);
    mvc.perform(
            put(path).principal(principal).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.changedBy").value(12))
        .andExpect(jsonPath("$.configurationVersion").value(1));
    when(service.forGateway(7, 3)).thenReturn(List.of(configuration));
    mvc.perform(get("/api/v1/organizations/7/gateways/3/source-profiles").principal(principal))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schemaVersion").value(1))
        .andExpect(jsonPath("$.sources[0].deviceUuid").value(configuration.deviceUuid()))
        .andExpect(jsonPath("$.sources[0].temperatureUnit").value("F"))
        .andExpect(jsonPath("$.sources[0].fields.temperature").value("temp"));
  }

  @Test
  void invalidRequestCannotReachService() throws Exception {
    permissions(7, "assets:manage");
    mvc.perform(
            put(path)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.replace("\"profileVersion\":1", "\"profileVersion\":0")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(path)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.replace("\"transport\":\"http\"", "\"transport\":\"ftp\"")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(path)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request.replace("\"expectedVersion\":0", "\"expectedVersion\":null")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
