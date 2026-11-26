package com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest;

import com.acme.coldtrace.platform.maintenancemanagement.application.commandservices.MaintenanceScheduleCommandService;
import com.acme.coldtrace.platform.maintenancemanagement.application.queryservices.MaintenanceScheduleQueryService;
import com.acme.coldtrace.platform.maintenancemanagement.domain.model.queries.GetMaintenanceScheduleByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.maintenancemanagement.domain.model.queries.GetMaintenanceSchedulesByOrganizationIdQuery;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.resources.CreateMaintenanceScheduleResource;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.resources.MaintenanceScheduleResource;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.resources.UpdateMaintenanceScheduleStatusResource;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.CreateMaintenanceScheduleCommandFromResourceAssembler;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.ResponseEntityFromMaintenanceScheduleCommandResultAssembler;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.ResponseEntityFromMaintenanceScheduleQueryResultAssembler;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.UpdateMaintenanceScheduleStatusCommandFromResourceAssembler;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing preventive maintenance schedule endpoints.
 * <p>
 * Maintenance schedules are organization-scoped planning resources associated
 * with cold-chain assets. The controller translates HTTP requests into
 * commands and queries while application services enforce ownership, duplicate
 * active schedules and lifecycle transitions.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/maintenance-schedules", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Maintenance Schedules", description = "Endpoints for preventive maintenance planning")
public class MaintenanceSchedulesController {
    private final MaintenanceScheduleCommandService maintenanceScheduleCommandService;
    private final MaintenanceScheduleQueryService maintenanceScheduleQueryService;
    private final MessageSource messageSource;

    public MaintenanceSchedulesController(
            MaintenanceScheduleCommandService maintenanceScheduleCommandService,
            MaintenanceScheduleQueryService maintenanceScheduleQueryService,
            MessageSource messageSource
    ) {
        this.maintenanceScheduleCommandService = maintenanceScheduleCommandService;
        this.maintenanceScheduleQueryService = maintenanceScheduleQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets maintenance schedules for an organization.
     *
     * @param organizationId organization identifier
     * @return response entity containing maintenance schedule resources
     */
    @Operation(summary = "Get maintenance schedules by organization",
            description = "Gets preventive maintenance schedules owned by the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance schedules found",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = MaintenanceScheduleResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getMaintenanceSchedulesByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/maintenance-schedules", organizationId);
        var result = maintenanceScheduleQueryService.handle(
                new GetMaintenanceSchedulesByOrganizationIdQuery(organizationId)
        );
        return ResponseEntityFromMaintenanceScheduleQueryResultAssembler.toResponseEntityFromListResult(
                result,
                messageSource
        );
    }

    /**
     * Gets one maintenance schedule by id.
     *
     * @param organizationId organization identifier
     * @param maintenanceScheduleId maintenance schedule identifier
     * @return response entity containing one maintenance schedule resource
     */
    @Operation(summary = "Get maintenance schedule by id",
            description = "Gets one preventive maintenance schedule owned by the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance schedule found",
                    content = @Content(schema = @Schema(implementation = MaintenanceScheduleResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or maintenance schedule not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{maintenanceScheduleId}")
    public ResponseEntity<?> getMaintenanceScheduleById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "maintenanceScheduleId", description = "Maintenance schedule identifier", required = true)
            @PathVariable Long maintenanceScheduleId) {
        log.debug("GET /organizations/{}/maintenance-schedules/{}", organizationId, maintenanceScheduleId);
        var result = maintenanceScheduleQueryService.handle(
                new GetMaintenanceScheduleByIdAndOrganizationIdQuery(organizationId, maintenanceScheduleId)
        );
        return ResponseEntityFromMaintenanceScheduleQueryResultAssembler.toResponseEntityFromScheduleResult(
                result,
                messageSource
        );
    }

    /**
     * Creates a preventive maintenance schedule.
     *
     * @param organizationId organization identifier
     * @param resource maintenance schedule creation request
     * @return response entity containing the created maintenance schedule
     */
    @Operation(
            summary = "Create a maintenance schedule",
            description = "Creates a preventive maintenance schedule for an organization asset",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Maintenance schedule creation request",
                    content = @Content(schema = @Schema(implementation = CreateMaintenanceScheduleResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Maintenance schedule created",
                    content = @Content(schema = @Schema(implementation = MaintenanceScheduleResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request or invalid scheduled date",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, asset or responsible user not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Asset already has an active maintenance schedule",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createMaintenanceSchedule(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateMaintenanceScheduleResource resource) {
        log.debug("POST /organizations/{}/maintenance-schedules - assetId={}", organizationId, resource.assetId());
        var command = CreateMaintenanceScheduleCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId
        );
        var result = maintenanceScheduleCommandService.handle(command);
        return ResponseEntityFromMaintenanceScheduleCommandResultAssembler.toResponseEntityFromCreateResult(
                result,
                messageSource
        );
    }

    /**
     * Updates the lifecycle status of one maintenance schedule.
     *
     * @param organizationId organization identifier
     * @param maintenanceScheduleId maintenance schedule identifier
     * @param resource status update request
     * @return response entity containing the updated maintenance schedule
     */
    @Operation(
            summary = "Update maintenance schedule status",
            description = "Updates the lifecycle status of a preventive maintenance schedule",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Maintenance schedule status update request",
                    content = @Content(schema = @Schema(
                            implementation = UpdateMaintenanceScheduleStatusResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance schedule updated",
                    content = @Content(schema = @Schema(implementation = MaintenanceScheduleResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request or invalid status",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or maintenance schedule not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Lifecycle transition is not allowed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{maintenanceScheduleId}")
    public ResponseEntity<?> updateMaintenanceScheduleStatus(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "maintenanceScheduleId", description = "Maintenance schedule identifier", required = true)
            @PathVariable Long maintenanceScheduleId,
            @Valid @RequestBody UpdateMaintenanceScheduleStatusResource resource) {
        log.debug("PATCH /organizations/{}/maintenance-schedules/{} - status={}",
                organizationId, maintenanceScheduleId, resource.status());
        var command = UpdateMaintenanceScheduleStatusCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                maintenanceScheduleId
        );
        var result = maintenanceScheduleCommandService.handle(command);
        return ResponseEntityFromMaintenanceScheduleCommandResultAssembler.toResponseEntityFromStatusUpdateResult(
                result,
                messageSource
        );
    }
}
