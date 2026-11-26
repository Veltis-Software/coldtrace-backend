package com.acme.coldtrace.platform.billing.interfaces.rest;

import com.acme.coldtrace.platform.billing.application.queryservices.SubscriptionPlanQueryService;
import com.acme.coldtrace.platform.billing.domain.model.queries.GetActiveSubscriptionPlansQuery;
import com.acme.coldtrace.platform.billing.interfaces.rest.resources.SubscriptionPlanResource;
import com.acme.coldtrace.platform.billing.interfaces.rest.transform.ResponseEntityFromSubscriptionPlanQueryResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing public subscription plan catalog endpoints.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/subscription-plans", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Subscription Plans", description = "Endpoints for public plan and pricing catalog")
public class SubscriptionPlansController {
    private final SubscriptionPlanQueryService subscriptionPlanQueryService;

    public SubscriptionPlansController(SubscriptionPlanQueryService subscriptionPlanQueryService) {
        this.subscriptionPlanQueryService = subscriptionPlanQueryService;
    }

    /**
     * Gets active subscription plans.
     *
     * @return response entity containing active plan resources
     */
    @Operation(
            summary = "Get subscription plans",
            description = "Gets active ColdTrace subscription plans with pricing, limits, and feature flags",
            security = {})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription plans found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubscriptionPlanResource.class))))
    })
    @GetMapping
    public ResponseEntity<?> getSubscriptionPlans() {
        log.debug("GET /api/v1/subscription-plans");
        var plans = subscriptionPlanQueryService.handle(new GetActiveSubscriptionPlansQuery());
        return ResponseEntityFromSubscriptionPlanQueryResultAssembler.toResponseEntityFromList(plans);
    }
}
