package com.acme.coldtrace.platform.billing.interfaces.rest;

import com.acme.coldtrace.platform.billing.application.commandservices.BillingPortalSessionCommandService;
import com.acme.coldtrace.platform.billing.interfaces.rest.resources.BillingPortalSessionResource;
import com.acme.coldtrace.platform.billing.interfaces.rest.transform.CreateBillingPortalSessionCommandFromResourceAssembler;
import com.acme.coldtrace.platform.billing.interfaces.rest.transform.ResponseEntityFromBillingPortalSessionCommandResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing billing customer portal session endpoints.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/billing/portal-sessions", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Billing Customer Portal Sessions", description = "Endpoints for provider-hosted billing management")
public class BillingPortalSessionsController {
    private final BillingPortalSessionCommandService billingPortalSessionCommandService;
    private final MessageSource messageSource;

    public BillingPortalSessionsController(
            BillingPortalSessionCommandService billingPortalSessionCommandService,
            MessageSource messageSource
    ) {
        this.billingPortalSessionCommandService = billingPortalSessionCommandService;
        this.messageSource = messageSource;
    }

    /**
     * Creates a provider-hosted customer portal session for billing management.
     *
     * @param organizationId organization identifier
     * @return customer portal session redirect response
     */
    @Operation(
            summary = "Create billing customer portal session",
            description = "Creates a provider-hosted Stripe Customer Portal session for an organization with Stripe billing state")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer portal session created",
                    content = @Content(schema = @Schema(implementation = BillingPortalSessionResource.class))),
            @ApiResponse(responseCode = "404", description = "Organization or subscription not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Organization has no provider customer identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Stripe customer portal provider failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Stripe customer portal provider is not configured",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createPortalSession(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId
    ) {
        log.debug("POST /api/v1/organizations/{}/billing/portal-sessions", organizationId);
        var command = CreateBillingPortalSessionCommandFromResourceAssembler.toCommandFromResource(organizationId);
        var result = billingPortalSessionCommandService.handle(command);
        return ResponseEntityFromBillingPortalSessionCommandResultAssembler.toResponseEntityFromResult(
                result,
                messageSource
        );
    }
}
