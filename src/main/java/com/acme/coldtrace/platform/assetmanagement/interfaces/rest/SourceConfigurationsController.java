package com.acme.coldtrace.platform.assetmanagement.interfaces.rest;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.SourceConfigurationException;
import com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices.SourceConfigurationService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.*;
import com.acme.coldtrace.platform.iam.interfaces.acl.SessionContextFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class SourceConfigurationsController {
  private final SourceConfigurationService service;
  private final SessionContextFacade sessions;

  public SourceConfigurationsController(
      SourceConfigurationService service, SessionContextFacade sessions) {
    this.service = service;
    this.sessions = sessions;
  }

  @PutMapping("/iot-devices/{deviceId}/source-configuration")
  public SourceConfiguration configure(
      @PathVariable long organizationId,
      @PathVariable long deviceId,
      @Valid @RequestBody ConfigurationRequest request,
      Authentication authentication) {
    var context = authorize(authentication, organizationId, true);
    SourceProfile profile;
    try {
      profile =
          new SourceProfile(
              request.sourceId(),
              request.transport(),
              request.profileVersion(),
              request.schemaVersion(),
              request.fields(),
              request.temperatureUnit(),
              request.humidityUnit());
    } catch (IllegalArgumentException invalid) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invalid.getMessage());
    }
    return service.configure(
        organizationId,
        deviceId,
        request.assetId(),
        request.expectedVersion(),
        profile,
        context.userId(),
        request.reason());
  }

  @GetMapping("/iot-devices/{deviceId}/source-configuration")
  public SourceConfiguration current(
      @PathVariable long organizationId,
      @PathVariable long deviceId,
      Authentication authentication) {
    authorize(authentication, organizationId, false);
    return service.current(organizationId, deviceId);
  }

  @GetMapping("/iot-devices/{deviceId}/source-configuration/history")
  public List<SourceConfiguration> history(
      @PathVariable long organizationId,
      @PathVariable long deviceId,
      Authentication authentication) {
    authorize(authentication, organizationId, false);
    return service.history(organizationId, deviceId);
  }

  /**
   * An authorized operator can provision this exact document to Edge; this is not device
   * authentication.
   */
  @GetMapping("/gateways/{gatewayId}/source-profiles")
  public Map<String, Object> export(
      @PathVariable long organizationId,
      @PathVariable long gatewayId,
      Authentication authentication) {
    authorize(authentication, organizationId, false);
    var sources =
        service.forGateway(organizationId, gatewayId).stream()
            .map(
                configuration -> {
                  var profile = configuration.profile();
                  return Map.<String, Object>of(
                      "sourceId",
                      profile.sourceId(),
                      "deviceUuid",
                      configuration.deviceUuid(),
                      "transport",
                      profile.transport(),
                      "profileVersion",
                      profile.profileVersion(),
                      "fields",
                      profile.fields(),
                      "temperatureUnit",
                      profile.temperatureUnit(),
                      "humidityUnit",
                      profile.humidityUnit());
                })
            .toList();
    return Map.of("schemaVersion", 1, "sources", sources);
  }

  private SessionContextFacade.SessionContext authorize(
      Authentication authentication, long organizationId, boolean write) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication
            instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var context =
        sessions
            .resolve(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    var permissions = context.permissions();
    boolean allowed =
        permissions.contains("assets:manage")
            || (!write
                && (permissions.contains("assets:view")
                    || permissions.contains("operations:view")));
    if (context.organizationId() == null
        || context.organizationId() != organizationId
        || !allowed) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return context;
  }

  @ExceptionHandler(SourceConfigurationException.class)
  public ResponseEntity<ProblemDetail> failure(SourceConfigurationException exception) {
    var status =
        switch (exception.kind()) {
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case CONFLICT -> HttpStatus.CONFLICT;
          case INVALID -> HttpStatus.BAD_REQUEST;
        };
    return ResponseEntity.status(status)
        .body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> duplicate() {
    return ResponseEntity.status(409)
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Source configuration conflicts with an existing association; reload before"
                    + " saving"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> authorizationFailure(ResponseStatusException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(ProblemDetail.forStatusAndDetail(exception.getStatusCode(),
            exception.getReason() == null ? "Access to this source configuration is not allowed" : exception.getReason()));
  }

  public record ConfigurationRequest(
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictLong.class)
          @NotNull
          @Positive
          Long assetId,
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictLong.class)
          @NotNull
          @PositiveOrZero
          Long expectedVersion,
      @NotBlank String sourceId,
      @NotBlank String transport,
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictInteger.class)
          @NotNull
          @Positive
          Integer profileVersion,
      @tools.jackson.databind.annotation.JsonDeserialize(using = StrictInteger.class)
          @NotNull
          @Min(1)
          @Max(1)
          Integer schemaVersion,
      @NotNull Map<String, String> fields,
      @NotBlank String temperatureUnit,
      @NotBlank String humidityUnit,
      @NotBlank @Size(max = 500) String reason) {}

  public static class StrictLong extends tools.jackson.databind.ValueDeserializer<Long> {
    public Long deserialize(
        tools.jackson.core.JsonParser parser,
        tools.jackson.databind.DeserializationContext context) {
      if (parser.currentToken() != tools.jackson.core.JsonToken.VALUE_NUMBER_INT)
        return context.reportInputMismatch(Long.class, "An integer JSON number is required");
      return parser.getLongValue();
    }
  }

  public static class StrictInteger extends tools.jackson.databind.ValueDeserializer<Integer> {
    public Integer deserialize(
        tools.jackson.core.JsonParser parser,
        tools.jackson.databind.DeserializationContext context) {
      if (parser.currentToken() != tools.jackson.core.JsonToken.VALUE_NUMBER_INT)
        return context.reportInputMismatch(Integer.class, "An integer JSON number is required");
      return parser.getIntValue();
    }
  }
}
