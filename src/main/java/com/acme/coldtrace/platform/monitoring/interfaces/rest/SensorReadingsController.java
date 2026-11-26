package com.acme.coldtrace.platform.monitoring.interfaces.rest;

import com.acme.coldtrace.platform.monitoring.application.commandservices.SensorReadingCommandService;
import com.acme.coldtrace.platform.monitoring.application.queryservices.SensorReadingQueryService;
import com.acme.coldtrace.platform.monitoring.domain.model.queries.GetSensorReadingByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.monitoring.domain.model.queries.GetSensorReadingsByOrganizationIdQuery;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.resources.CreateSensorReadingResource;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.resources.GenerateDemoSensorReadingsResource;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.resources.SensorReadingResource;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.transform.CreateSensorReadingCommandFromResourceAssembler;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.transform.GenerateDemoSensorReadingsCommandFromResourceAssembler;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.transform.ResponseEntityFromSensorReadingCommandResultAssembler;
import com.acme.coldtrace.platform.monitoring.interfaces.rest.transform.ResponseEntityFromSensorReadingQueryResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing sensor reading endpoints.
 * <p>
 * Reading operations are organization-scoped and backend-owned. The controller
 * remains thin by translating HTTP input into command and query objects while
 * application services validate devices, assets, gateways and thresholds.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/sensor-readings", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Sensor Readings", description = "Endpoints for persisted telemetry readings")
public class SensorReadingsController {
    private final SensorReadingCommandService sensorReadingCommandService;
    private final SensorReadingQueryService sensorReadingQueryService;
    private final MessageSource messageSource;

    public SensorReadingsController(
            SensorReadingCommandService sensorReadingCommandService,
            SensorReadingQueryService sensorReadingQueryService,
            MessageSource messageSource
    ) {
        this.sensorReadingCommandService = sensorReadingCommandService;
        this.sensorReadingQueryService = sensorReadingQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets sensor readings for an organization.
     *
     * @param organizationId organization identifier
     * @param assetId optional asset filter
     * @param iotDeviceId optional device filter
     * @param from optional lower timestamp bound
     * @param to optional upper timestamp bound
     * @return response entity containing reading resources
     */
    @Operation(summary = "Get sensor readings",
            description = "Gets persisted telemetry readings for an organization with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Readings found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SensorReadingResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid filter",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getSensorReadings(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Long iotDeviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        log.debug("GET /organizations/{}/sensor-readings", organizationId);
        var result = sensorReadingQueryService.handle(
                new GetSensorReadingsByOrganizationIdQuery(organizationId, assetId, iotDeviceId, from, to)
        );
        return ResponseEntityFromSensorReadingQueryResultAssembler.toResponseEntityFromListResult(
                result,
                messageSource
        );
    }

    /**
     * Gets one sensor reading by id.
     *
     * @param organizationId organization identifier
     * @param sensorReadingId reading identifier
     * @return response entity containing one reading resource
     */
    @Operation(summary = "Get sensor reading by id",
            description = "Gets one persisted telemetry reading owned by the organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading found",
                    content = @Content(schema = @Schema(implementation = SensorReadingResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or reading not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{sensorReadingId}")
    public ResponseEntity<?> getSensorReadingById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "sensorReadingId", description = "Sensor reading identifier", required = true)
            @PathVariable Long sensorReadingId) {
        log.debug("GET /organizations/{}/sensor-readings/{}", organizationId, sensorReadingId);
        var result = sensorReadingQueryService.handle(
                new GetSensorReadingByIdAndOrganizationIdQuery(organizationId, sensorReadingId)
        );
        return ResponseEntityFromSensorReadingQueryResultAssembler.toResponseEntityFromReadingResult(
                result,
                messageSource
        );
    }

    /**
     * Creates a sensor reading explicitly.
     *
     * @param organizationId organization identifier
     * @param resource reading creation request
     * @return response entity containing created reading
     */
    @Operation(
            summary = "Create a sensor reading",
            description = "Persists telemetry from an assigned online IoT device and evaluates it against asset settings",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Sensor reading creation request",
                    content = @Content(schema = @Schema(implementation = CreateSensorReadingResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reading created",
                    content = @Content(schema = @Schema(implementation = SensorReadingResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request or invalid telemetry context",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, asset, device or gateway not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createSensorReading(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateSensorReadingResource resource) {
        log.debug("POST /organizations/{}/sensor-readings", organizationId);
        var command = CreateSensorReadingCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var result = sensorReadingCommandService.handle(command);
        return ResponseEntityFromSensorReadingCommandResultAssembler.toResponseEntityFromCreateResult(
                result,
                messageSource
        );
    }

    /**
     * Generates demo sensor readings in the backend.
     *
     * @param organizationId organization identifier
     * @param resource generation request
     * @return response entity containing generated readings
     */
    @Operation(
            summary = "Generate demo sensor readings",
            description = "Generates and persists realistic readings for eligible assigned online IoT devices",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Demo generation request",
                    content = @Content(schema = @Schema(implementation = GenerateDemoSensorReadingsResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Readings generated",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SensorReadingResource.class)))),
            @ApiResponse(responseCode = "400", description = "No eligible device or invalid generation request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/demo-generations")
    public ResponseEntity<?> generateDemoSensorReadings(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @RequestBody(required = false) GenerateDemoSensorReadingsResource resource) {
        log.debug("POST /organizations/{}/sensor-readings/demo-generations", organizationId);
        var command = GenerateDemoSensorReadingsCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId
        );
        var result = sensorReadingCommandService.handle(command);
        return ResponseEntityFromSensorReadingCommandResultAssembler.toResponseEntityFromGenerationResult(
                result,
                messageSource
        );
    }
}
