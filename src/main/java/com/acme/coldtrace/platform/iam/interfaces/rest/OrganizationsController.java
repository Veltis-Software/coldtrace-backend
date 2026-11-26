package com.acme.coldtrace.platform.iam.interfaces.rest;

import com.acme.coldtrace.platform.iam.application.commandservices.OrganizationCommandService;
import com.acme.coldtrace.platform.iam.application.queryservices.OrganizationQueryService;
import com.acme.coldtrace.platform.iam.domain.model.queries.GetAllOrganizationsQuery;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.CreateOrganizationResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.OrganizationResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.CreateOrganizationCommandFromResourceAssembler;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.ResponseEntityFromOrganizationCommandResultAssembler;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.ResponseEntityFromOrganizationQueryResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing organization endpoints.
 * It translates HTTP requests into application commands and queries for the
 * IAM bounded context.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Organizations", description = "Endpoints for organizations")
public class OrganizationsController {
    private final OrganizationCommandService organizationCommandService;
    private final OrganizationQueryService organizationQueryService;
    private final MessageSource messageSource;

    public OrganizationsController(
            OrganizationCommandService organizationCommandService,
            OrganizationQueryService organizationQueryService,
            MessageSource messageSource
    ) {
        this.organizationCommandService = organizationCommandService;
        this.organizationQueryService = organizationQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets all organizations.
     *
     * @return response entity containing organization resources
     */
    @Operation(summary = "Get all organizations", description = "Gets the organizations registered in the platform")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organizations found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationResource.class))))
    })
    @GetMapping
    public ResponseEntity<?> getAllOrganizations() {
        log.debug("GET /organizations");
        var organizations = organizationQueryService.handle(new GetAllOrganizationsQuery());
        return ResponseEntityFromOrganizationQueryResultAssembler.toResponseEntityFromList(organizations);
    }

    /**
     * Creates an organization.
     *
     * @param resource organization creation request resource
     * @return response entity containing the created organization resource
     */
    @Operation(
            summary = "Create an organization",
            description = "Creates an organization with the provided legal, commercial, and contact data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Organization creation request",
                    content = @Content(schema = @Schema(implementation = CreateOrganizationResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Organization created",
                    content = @Content(schema = @Schema(implementation = OrganizationResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - organization already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createOrganization(@Valid @RequestBody CreateOrganizationResource resource) {
        log.debug("POST /organizations - contactEmail={}", resource.contactEmail());
        var command = CreateOrganizationCommandFromResourceAssembler.toCommandFromResource(resource);
        var organization = organizationCommandService.handle(command);
        return ResponseEntityFromOrganizationCommandResultAssembler.toResponseEntityFromResult(organization, messageSource);
    }
}
