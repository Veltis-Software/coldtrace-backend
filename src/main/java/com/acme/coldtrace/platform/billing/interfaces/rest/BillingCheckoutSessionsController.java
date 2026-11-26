package com.acme.coldtrace.platform.billing.interfaces.rest;

import com.acme.coldtrace.platform.billing.application.commandservices.BillingCheckoutSessionCommandService;
import com.acme.coldtrace.platform.billing.interfaces.rest.resources.BillingCheckoutSessionResource;
import com.acme.coldtrace.platform.billing.interfaces.rest.resources.CreateBillingCheckoutSessionResource;
import com.acme.coldtrace.platform.billing.interfaces.rest.transform.CreateBillingCheckoutSessionCommandFromResourceAssembler;
import com.acme.coldtrace.platform.billing.interfaces.rest.transform.ResponseEntityFromBillingCheckoutSessionCommandResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing billing checkout session endpoints.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/billing/checkout-sessions", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Billing Checkout Sessions", description = "Endpoints for provider-hosted billing checkout")
public class BillingCheckoutSessionsController {
    private final BillingCheckoutSessionCommandService billingCheckoutSessionCommandService;
    private final MessageSource messageSource;

    public BillingCheckoutSessionsController(
            BillingCheckoutSessionCommandService billingCheckoutSessionCommandService,
            MessageSource messageSource
    ) {
        this.billingCheckoutSessionCommandService = billingCheckoutSessionCommandService;
        this.messageSource = messageSource;
    }

    /**
     * Creates a provider-hosted checkout session for paid plan upgrade.
     *
     * @param organizationId organization identifier
     * @param resource checkout session creation resource
     * @return checkout session redirect response
     */
    @Operation(
            summary = "Create billing checkout session",
            description = "Creates a provider-hosted Stripe Checkout session for a paid ColdTrace plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Checkout session created",
                    content = @Content(schema = @Schema(implementation = BillingCheckoutSessionResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid request data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, subscription, or target plan not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Plan is not eligible or configured for checkout",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Stripe checkout provider failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Stripe checkout provider is not configured",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCheckoutSession(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateBillingCheckoutSessionResource resource
    ) {
        log.debug("POST /api/v1/organizations/{}/billing/checkout-sessions", organizationId);
        var command = CreateBillingCheckoutSessionCommandFromResourceAssembler.toCommandFromResource(
                organizationId,
                resource
        );
        var result = billingCheckoutSessionCommandService.handle(command);
        return ResponseEntityFromBillingCheckoutSessionCommandResultAssembler.toResponseEntityFromResult(
                result,
                messageSource
        );
    }
}
