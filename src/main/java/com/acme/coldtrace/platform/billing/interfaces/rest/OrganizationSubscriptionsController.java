package com.acme.coldtrace.platform.billing.interfaces.rest;

import com.acme.coldtrace.platform.billing.application.queryservices.OrganizationSubscriptionQueryService;
import com.acme.coldtrace.platform.billing.domain.model.queries.GetOrganizationSubscriptionByOrganizationIdQuery;
import com.acme.coldtrace.platform.billing.interfaces.rest.resources.OrganizationSubscriptionResource;
import com.acme.coldtrace.platform.billing.interfaces.rest.transform.ResponseEntityFromOrganizationSubscriptionQueryResultAssembler;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing organization subscription endpoints.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/subscription", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Organization Subscriptions", description = "Endpoints for organization subscription and entitlements")
public class OrganizationSubscriptionsController {
    private final OrganizationSubscriptionQueryService organizationSubscriptionQueryService;
    private final MessageSource messageSource;

    public OrganizationSubscriptionsController(
            OrganizationSubscriptionQueryService organizationSubscriptionQueryService,
            MessageSource messageSource
    ) {
        this.organizationSubscriptionQueryService = organizationSubscriptionQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets current subscription and entitlements for an organization.
     *
     * @param organizationId organization identifier
     * @return response entity containing subscription details
     */
    @Operation(
            summary = "Get organization subscription",
            description = "Gets current plan, subscription state, usage counters, and computed entitlements")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Organization subscription found",
                    content = @Content(schema = @Schema(implementation = OrganizationSubscriptionResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or subscription plan not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getOrganizationSubscription(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId
    ) {
        log.debug("GET /api/v1/organizations/{}/subscription", organizationId);
        var result = organizationSubscriptionQueryService.handle(
                new GetOrganizationSubscriptionByOrganizationIdQuery(organizationId)
        );
        return ResponseEntityFromOrganizationSubscriptionQueryResultAssembler.toResponseEntityFromResult(
                result,
                messageSource
        );
    }
}
