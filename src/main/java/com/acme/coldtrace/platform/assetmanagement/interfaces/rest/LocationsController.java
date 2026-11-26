package com.acme.coldtrace.platform.assetmanagement.interfaces.rest;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.LocationCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteLocationCommand;
import com.acme.coldtrace.platform.assetmanagement.application.queryservices.LocationQueryService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetLocationByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetLocationsByOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.CreateLocationResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.LocationResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.UpdateLocationResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.CreateLocationCommandFromResourceAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromLocationCommandResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromLocationQueryResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.UpdateLocationCommandFromResourceAssembler;
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
 * REST controller exposing location endpoints.
 * It translates HTTP requests into location commands and queries.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/locations", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Locations", description = "Endpoints for operational locations")
public class LocationsController {
    private final LocationCommandService locationCommandService;
    private final LocationQueryService locationQueryService;
    private final MessageSource messageSource;

    public LocationsController(
            LocationCommandService locationCommandService,
            LocationQueryService locationQueryService,
            MessageSource messageSource
    ) {
        this.locationCommandService = locationCommandService;
        this.locationQueryService = locationQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets locations that belong to an organization.
     *
     * @param organizationId organization identifier used to filter locations
     * @return response entity containing location resources
     */
    @Operation(summary = "Get locations by organization",
            description = "Gets operational locations that belong to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Locations found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LocationResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getLocationsByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/locations", organizationId);
        var locations = locationQueryService.handle(new GetLocationsByOrganizationIdQuery(organizationId));
        return ResponseEntityFromLocationQueryResultAssembler.toResponseEntityFromList(locations);
    }

    /**
     * Gets one location by id.
     *
     * @param organizationId organization identifier
     * @param locationId location identifier
     * @return response entity containing one location resource
     */
    @Operation(summary = "Get location by id",
            description = "Gets one operational location that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location found",
                    content = @Content(schema = @Schema(implementation = LocationResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or location not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{locationId}")
    public ResponseEntity<?> getLocationById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "locationId", description = "Location identifier", required = true)
            @PathVariable Long locationId) {
        log.debug("GET /organizations/{}/locations/{}", organizationId, locationId);
        var location = locationQueryService.handle(
                new GetLocationByIdAndOrganizationIdQuery(organizationId, locationId)
        );
        if (location.isEmpty()) {
            log.debug("Location not found for organizationId={}, locationId={}", organizationId, locationId);
            return ResponseEntityFromLocationQueryResultAssembler.notFound(
                    messageSource,
                    "asset-management.location.error.location-not-found"
            );
        }
        return ResponseEntityFromLocationQueryResultAssembler.toResponseEntityFromLocation(location.orElseThrow());
    }

    /**
     * Creates a location.
     *
     * @param organizationId organization identifier
     * @param resource location creation request resource
     * @return response entity containing the created location resource
     */
    @Operation(
            summary = "Create a location",
            description = "Creates an operational location for an organization",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Location creation request",
                    content = @Content(schema = @Schema(implementation = CreateLocationResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Location created",
                    content = @Content(schema = @Schema(implementation = LocationResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - location name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createLocation(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateLocationResource resource) {
        log.debug("POST /organizations/{}/locations - name={}", organizationId, resource.name());
        var command = CreateLocationCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var location = locationCommandService.handle(command);
        return ResponseEntityFromLocationCommandResultAssembler.toResponseEntityFromCreateResult(
                location,
                messageSource
        );
    }

    /**
     * Updates a location.
     *
     * @param organizationId organization identifier
     * @param locationId location identifier
     * @param resource location update request resource
     * @return response entity containing the updated location resource
     */
    @Operation(
            summary = "Update a location",
            description = "Updates an operational location for an organization",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Location update request",
                    content = @Content(schema = @Schema(implementation = UpdateLocationResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Location updated",
                    content = @Content(schema = @Schema(implementation = LocationResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or location not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - location name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{locationId}")
    public ResponseEntity<?> updateLocation(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "locationId", description = "Location identifier", required = true)
            @PathVariable Long locationId,
            @Valid @RequestBody UpdateLocationResource resource) {
        log.debug("PUT /organizations/{}/locations/{} - name={}",
                organizationId, locationId, resource.name());
        var command = UpdateLocationCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                locationId
        );
        var location = locationCommandService.handle(command);
        return ResponseEntityFromLocationCommandResultAssembler.toResponseEntityFromUpdateResult(
                location,
                messageSource
        );
    }

    /**
     * Deletes a location.
     *
     * @param organizationId organization identifier
     * @param locationId location identifier
     * @return empty response on success or failure detail
     */
    @Operation(summary = "Delete a location",
            description = "Deletes one operational location that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Location deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or location not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Location cannot be deleted because related data exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{locationId}")
    public ResponseEntity<?> deleteLocation(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "locationId", description = "Location identifier", required = true)
            @PathVariable Long locationId) {
        log.debug("DELETE /organizations/{}/locations/{}", organizationId, locationId);
        var result = locationCommandService.handle(new DeleteLocationCommand(organizationId, locationId));
        return ResponseEntityFromLocationCommandResultAssembler.toResponseEntityFromDeleteResult(
                result,
                messageSource
        );
    }
}
