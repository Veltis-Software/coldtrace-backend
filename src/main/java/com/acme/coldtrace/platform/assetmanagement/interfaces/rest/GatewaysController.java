package com.acme.coldtrace.platform.assetmanagement.interfaces.rest;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.GatewayCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteGatewayCommand;
import com.acme.coldtrace.platform.assetmanagement.application.queryservices.GatewayQueryService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetGatewayByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetGatewaysByOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.CreateGatewayResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.GatewayResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.UpdateGatewayResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.CreateGatewayCommandFromResourceAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromGatewayCommandResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromGatewayQueryResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.UpdateGatewayCommandFromResourceAssembler;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing gateway endpoints.
 * It translates HTTP requests into gateway commands and queries.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/gateways", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Gateways", description = "Endpoints for edge gateways")
public class GatewaysController {
    private final GatewayCommandService gatewayCommandService;
    private final GatewayQueryService gatewayQueryService;
    private final MessageSource messageSource;

    public GatewaysController(
            GatewayCommandService gatewayCommandService,
            GatewayQueryService gatewayQueryService,
            MessageSource messageSource
    ) {
        this.gatewayCommandService = gatewayCommandService;
        this.gatewayQueryService = gatewayQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets gateways that belong to an organization.
     *
     * @param organizationId organization identifier used to filter gateways
     * @return response entity containing gateway resources
     */
    @Operation(summary = "Get gateways by organization",
            description = "Gets edge gateways that belong to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gateways found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GatewayResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getGatewaysByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/gateways", organizationId);
        var gateways = gatewayQueryService.handle(new GetGatewaysByOrganizationIdQuery(organizationId));
        return ResponseEntityFromGatewayQueryResultAssembler.toResponseEntityFromList(gateways);
    }

    /**
     * Gets one gateway by id.
     *
     * @param organizationId organization identifier
     * @param gatewayId gateway identifier
     * @return response entity containing one gateway resource
     */
    @Operation(summary = "Get gateway by id",
            description = "Gets one edge gateway that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gateway found",
                    content = @Content(schema = @Schema(implementation = GatewayResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or gateway not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{gatewayId}")
    public ResponseEntity<?> getGatewayById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "gatewayId", description = "Gateway identifier", required = true)
            @PathVariable Long gatewayId) {
        log.debug("GET /organizations/{}/gateways/{}", organizationId, gatewayId);
        var gateway = gatewayQueryService.handle(
                new GetGatewayByIdAndOrganizationIdQuery(organizationId, gatewayId)
        );
        if (gateway.isEmpty()) {
            log.debug("Gateway not found for organizationId={}, gatewayId={}", organizationId, gatewayId);
            return ResponseEntityFromGatewayQueryResultAssembler.notFound(
                    messageSource,
                    "asset-management.gateway.error.gateway-not-found"
            );
        }
        return ResponseEntityFromGatewayQueryResultAssembler.toResponseEntityFromGateway(gateway.orElseThrow());
    }

    /**
     * Creates a gateway.
     *
     * @param organizationId organization identifier
     * @param resource gateway creation request resource
     * @return response entity containing the created gateway resource
     */
    @Operation(
            summary = "Create a gateway",
            description = "Creates an edge gateway for an organization location",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Gateway creation request",
                    content = @Content(schema = @Schema(implementation = CreateGatewayResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Gateway created",
                    content = @Content(schema = @Schema(implementation = GatewayResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or location not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - gateway uuid already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createGateway(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateGatewayResource resource) {
        log.debug("POST /organizations/{}/gateways - uuid={}", organizationId, resource.uuid());
        var command = CreateGatewayCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var gateway = gatewayCommandService.handle(command);
        return ResponseEntityFromGatewayCommandResultAssembler.toResponseEntityFromCreateResult(
                gateway,
                messageSource
        );
    }

    /**
     * Updates a gateway.
     *
     * @param organizationId organization identifier
     * @param gatewayId gateway identifier
     * @param resource gateway update request resource
     * @return response entity containing the updated gateway resource
     */
    @Operation(
            summary = "Update a gateway",
            description = "Updates an edge gateway for an organization location",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Gateway update request",
                    content = @Content(schema = @Schema(implementation = UpdateGatewayResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gateway updated",
                    content = @Content(schema = @Schema(implementation = GatewayResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, location or gateway not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - gateway uuid already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{gatewayId}")
    public ResponseEntity<?> updateGateway(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "gatewayId", description = "Gateway identifier", required = true)
            @PathVariable Long gatewayId,
            @Valid @RequestBody UpdateGatewayResource resource) {
        log.debug("PUT /organizations/{}/gateways/{} - uuid={}",
                organizationId, gatewayId, resource.uuid());
        var command = UpdateGatewayCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                gatewayId
        );
        var gateway = gatewayCommandService.handle(command);
        return ResponseEntityFromGatewayCommandResultAssembler.toResponseEntityFromUpdateResult(
                gateway,
                messageSource
        );
    }

    /**
     * Deletes a gateway.
     *
     * @param organizationId organization identifier
     * @param gatewayId gateway identifier
     * @return empty response on success or failure detail
     */
    @Operation(summary = "Delete a gateway",
            description = "Deletes one edge gateway that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Gateway deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or gateway not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Gateway cannot be deleted because related data exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{gatewayId}")
    public ResponseEntity<?> deleteGateway(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "gatewayId", description = "Gateway identifier", required = true)
            @PathVariable Long gatewayId) {
        log.debug("DELETE /organizations/{}/gateways/{}", organizationId, gatewayId);
        var result = gatewayCommandService.handle(new DeleteGatewayCommand(organizationId, gatewayId));
        return ResponseEntityFromGatewayCommandResultAssembler.toResponseEntityFromDeleteResult(
                result,
                messageSource
        );
    }
}
