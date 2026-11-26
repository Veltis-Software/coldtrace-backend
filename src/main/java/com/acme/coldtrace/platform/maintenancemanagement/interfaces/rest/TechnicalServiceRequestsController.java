package com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest;

import com.acme.coldtrace.platform.maintenancemanagement.application.commandservices.TechnicalServiceRequestCommandService;
import com.acme.coldtrace.platform.maintenancemanagement.application.queryservices.TechnicalServiceRequestQueryService;
import com.acme.coldtrace.platform.maintenancemanagement.domain.model.queries.GetTechnicalServiceRequestByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.maintenancemanagement.domain.model.queries.GetTechnicalServiceRequestsByOrganizationIdQuery;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.resources.CreateTechnicalServiceRequestResource;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.resources.TechnicalServiceRequestResource;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.resources.UpdateTechnicalServiceRequestStatusResource;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.CreateTechnicalServiceRequestCommandFromResourceAssembler;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.ResponseEntityFromTechnicalServiceRequestCommandResultAssembler;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.ResponseEntityFromTechnicalServiceRequestQueryResultAssembler;
import com.acme.coldtrace.platform.maintenancemanagement.interfaces.rest.transform.UpdateTechnicalServiceRequestStatusCommandFromResourceAssembler;
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
 * REST controller that exposes technical service request operations.
 * <p>
 * Technical service requests represent corrective maintenance work requested for
 * monitored assets. Routes are nested under organizations to keep every operation
 * tenant-scoped and consistent with the platform REST conventions.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/technical-service-requests", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Technical Service Requests", description = "Endpoints for corrective maintenance service requests")
public class TechnicalServiceRequestsController {
    private final TechnicalServiceRequestCommandService technicalServiceRequestCommandService;
    private final TechnicalServiceRequestQueryService technicalServiceRequestQueryService;
    private final MessageSource messageSource;

    /**
     * Creates the controller with command and query services required by the REST API.
     *
     * @param technicalServiceRequestCommandService command service for write operations
     * @param technicalServiceRequestQueryService query service for read operations
     * @param messageSource message source used by response assemblers to localize errors
     */
    public TechnicalServiceRequestsController(
            TechnicalServiceRequestCommandService technicalServiceRequestCommandService,
            TechnicalServiceRequestQueryService technicalServiceRequestQueryService,
            MessageSource messageSource
    ) {
        this.technicalServiceRequestCommandService = technicalServiceRequestCommandService;
        this.technicalServiceRequestQueryService = technicalServiceRequestQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets all technical service requests registered for an organization.
     *
     * @param organizationId identifier of the organization that owns the requests
     * @return list of technical service request resources or a problem response
     */
    @Operation(summary = "Get technical service requests by organization")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Technical service requests found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TechnicalServiceRequestResource.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping
    public ResponseEntity<?> getTechnicalServiceRequestsByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId
    ) {
        log.debug("GET /organizations/{}/technical-service-requests", organizationId);
        var result = technicalServiceRequestQueryService.handle(
                new GetTechnicalServiceRequestsByOrganizationIdQuery(organizationId)
        );
        return ResponseEntityFromTechnicalServiceRequestQueryResultAssembler.toResponseEntityFromListResult(
                result,
                messageSource
        );
    }

    /**
     * Gets one technical service request by id within an organization.
     *
     * @param organizationId identifier of the organization that owns the request
     * @param technicalServiceRequestId identifier of the technical service request
     * @return technical service request resource or a problem response
     */
    @Operation(summary = "Get technical service request by id")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Technical service request found",
                    content = @Content(schema = @Schema(implementation = TechnicalServiceRequestResource.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization or technical service request not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{technicalServiceRequestId}")
    public ResponseEntity<?> getTechnicalServiceRequestById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "technicalServiceRequestId", description = "Technical service request identifier", required = true)
            @PathVariable Long technicalServiceRequestId
    ) {
        log.debug("GET /organizations/{}/technical-service-requests/{}", organizationId, technicalServiceRequestId);
        var result = technicalServiceRequestQueryService.handle(
                new GetTechnicalServiceRequestByIdAndOrganizationIdQuery(
                        organizationId,
                        technicalServiceRequestId
                )
        );
        return ResponseEntityFromTechnicalServiceRequestQueryResultAssembler.toResponseEntityFromRequestResult(
                result,
                messageSource
        );
    }

    /**
     * Creates a technical service request for an asset.
     *
     * @param organizationId identifier of the organization that owns the asset
     * @param resource request body with the asset, optional incident and issue description
     * @return created technical service request resource or a problem response
     */
    @Operation(summary = "Create a technical service request")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Technical service request created",
                    content = @Content(schema = @Schema(implementation = TechnicalServiceRequestResource.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization, asset or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createTechnicalServiceRequest(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateTechnicalServiceRequestResource resource
    ) {
        log.debug("POST /organizations/{}/technical-service-requests - assetId={}", organizationId, resource.assetId());
        var command = CreateTechnicalServiceRequestCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId
        );
        var result = technicalServiceRequestCommandService.handle(command);
        return ResponseEntityFromTechnicalServiceRequestCommandResultAssembler.toResponseEntityFromCreateResult(
                result,
                messageSource
        );
    }

    /**
     * Updates the lifecycle status of a technical service request.
     *
     * @param organizationId identifier of the organization that owns the request
     * @param technicalServiceRequestId identifier of the technical service request
     * @param resource request body with the target status and optional closure data
     * @return updated technical service request resource or a problem response
     */
    @Operation(summary = "Update technical service request status")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Technical service request updated",
                    content = @Content(schema = @Schema(implementation = TechnicalServiceRequestResource.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request or missing closure data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization or technical service request not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Lifecycle transition is not allowed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PatchMapping("/{technicalServiceRequestId}")
    public ResponseEntity<?> updateTechnicalServiceRequestStatus(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "technicalServiceRequestId", description = "Technical service request identifier", required = true)
            @PathVariable Long technicalServiceRequestId,
            @Valid @RequestBody UpdateTechnicalServiceRequestStatusResource resource
    ) {
        log.debug(
                "PATCH /organizations/{}/technical-service-requests/{} - status={}",
                organizationId,
                technicalServiceRequestId,
                resource.status()
        );
        var command = UpdateTechnicalServiceRequestStatusCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                technicalServiceRequestId
        );
        var result = technicalServiceRequestCommandService.handle(command);
        return ResponseEntityFromTechnicalServiceRequestCommandResultAssembler.toResponseEntityFromUpdateResult(
                result,
                messageSource
        );
    }
}
