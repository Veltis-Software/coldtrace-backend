package com.acme.coldtrace.platform.iam.interfaces.rest;

import com.acme.coldtrace.platform.iam.application.commandservices.OrganizationSignUpCommandService;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.CreateOrganizationSignUpResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.OrganizationSignUpResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.CreateOrganizationSignUpCommandFromResourceAssembler;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.ResponseEntityFromOrganizationSignUpCommandResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing the organization sign-up endpoint.
 * It creates an organization and its first user through one application command.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organization-sign-ups", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Organization Sign-Ups", description = "Endpoints for organization sign-up")
public class OrganizationSignUpsController {
    private final OrganizationSignUpCommandService organizationSignUpCommandService;
    private final MessageSource messageSource;

    public OrganizationSignUpsController(
            OrganizationSignUpCommandService organizationSignUpCommandService,
            MessageSource messageSource
    ) {
        this.organizationSignUpCommandService = organizationSignUpCommandService;
        this.messageSource = messageSource;
    }

    /**
     * Signs up an organization and its first user.
     *
     * @param resource organization sign-up request resource
     * @return response entity containing the created organization and first user, or failure detail
     */
    @Operation(
            summary = "Sign up an organization",
            description = "Creates an organization and its first super administrator user in one transaction",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Organization sign-up request",
                    content = @Content(schema = @Schema(implementation = CreateOrganizationSignUpResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Organization sign-up completed",
                    content = @Content(schema = @Schema(implementation = OrganizationSignUpResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - organization or user already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Initial role is missing",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> signUpOrganization(@Valid @RequestBody CreateOrganizationSignUpResource resource) {
        log.debug("POST /organization-sign-ups - organizationContactEmail={}, userEmail={}",
                resource.contactEmail(), resource.email());
        var command = CreateOrganizationSignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var signUp = organizationSignUpCommandService.handle(command);
        return ResponseEntityFromOrganizationSignUpCommandResultAssembler.toResponseEntityFromResult(signUp, messageSource);
    }
}
