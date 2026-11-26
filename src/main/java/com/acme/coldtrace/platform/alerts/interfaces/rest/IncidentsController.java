package com.acme.coldtrace.platform.alerts.interfaces.rest;

import com.acme.coldtrace.platform.alerts.application.commandservices.IncidentCommandService;
import com.acme.coldtrace.platform.alerts.application.commandservices.AiResolutionPlanCommandService;
import com.acme.coldtrace.platform.alerts.application.queryservices.AiResolutionPlanQueryService;
import com.acme.coldtrace.platform.alerts.application.queryservices.IncidentQueryService;
import com.acme.coldtrace.platform.alerts.application.queryservices.NotificationQueryService;
import com.acme.coldtrace.platform.alerts.domain.model.commands.GenerateAiResolutionPlanCommand;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetIncidentByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetIncidentsByOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetNotificationsByIncidentIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.AiResolutionPlanResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.AcknowledgeIncidentResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.ApproveAiResolutionPlanResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.CreateIncidentResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.EscalateIncidentResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.IncidentResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.NotificationResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.RegisterIncidentCorrectiveActionResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.RejectAiResolutionPlanResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.ResolveIncidentResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.AcknowledgeIncidentCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ApproveAiResolutionPlanCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.CreateIncidentCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.EscalateIncidentCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.RegisterIncidentCorrectiveActionCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.RejectAiResolutionPlanCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResolveIncidentCommandFromResourceAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResponseEntityFromAiResolutionPlanCommandResultAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResponseEntityFromAiResolutionPlanQueryResultAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResponseEntityFromIncidentCommandResultAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResponseEntityFromIncidentQueryResultAssembler;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResponseEntityFromNotificationQueryResultAssembler;
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
 * REST controller exposing incident endpoints.
 * It translates HTTP requests into alert commands and queries.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/incidents", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Incidents", description = "Endpoints for organization-scoped incidents")
public class IncidentsController {
    private final IncidentCommandService incidentCommandService;
    private final AiResolutionPlanCommandService aiResolutionPlanCommandService;
    private final IncidentQueryService incidentQueryService;
    private final AiResolutionPlanQueryService aiResolutionPlanQueryService;
    private final NotificationQueryService notificationQueryService;
    private final MessageSource messageSource;

    public IncidentsController(
            IncidentCommandService incidentCommandService,
            AiResolutionPlanCommandService aiResolutionPlanCommandService,
            IncidentQueryService incidentQueryService,
            AiResolutionPlanQueryService aiResolutionPlanQueryService,
            NotificationQueryService notificationQueryService,
            MessageSource messageSource
    ) {
        this.incidentCommandService = incidentCommandService;
        this.aiResolutionPlanCommandService = aiResolutionPlanCommandService;
        this.incidentQueryService = incidentQueryService;
        this.aiResolutionPlanQueryService = aiResolutionPlanQueryService;
        this.notificationQueryService = notificationQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets incidents that belong to an organization.
     *
     * @param organizationId organization identifier used to filter incidents
     * @return response entity containing incident resources
     */
    @Operation(summary = "Get incidents by organization",
            description = "Gets incidents owned by the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incidents found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = IncidentResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getIncidentsByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/incidents", organizationId);
        var incidents = incidentQueryService.handle(new GetIncidentsByOrganizationIdQuery(organizationId));
        return ResponseEntityFromIncidentQueryResultAssembler.toResponseEntityFromListResult(incidents, messageSource);
    }

    /**
     * Gets one incident by id.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @return response entity containing one incident resource
     */
    @Operation(summary = "Get incident by id",
            description = "Gets one incident owned by the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident found",
                    content = @Content(schema = @Schema(implementation = IncidentResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{incidentId}")
    public ResponseEntity<?> getIncidentById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId) {
        log.debug("GET /organizations/{}/incidents/{}", organizationId, incidentId);
        var incident = incidentQueryService.handle(
                new GetIncidentByIdAndOrganizationIdQuery(organizationId, incidentId)
        );
        return ResponseEntityFromIncidentQueryResultAssembler.toResponseEntityFromIncidentResult(incident, messageSource);
    }

    /**
     * Creates a manual incident.
     *
     * @param organizationId organization identifier
     * @param resource incident creation request resource
     * @return response entity containing the created incident resource
     */
    @Operation(
            summary = "Create an incident",
            description = "Registers an incident owned by the organization. Generated incident flows can call the same application command.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Incident creation request",
                    content = @Content(schema = @Schema(implementation = CreateIncidentResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Incident created",
                    content = @Content(schema = @Schema(implementation = IncidentResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createIncident(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateIncidentResource resource) {
        log.debug("POST /organizations/{}/incidents - type={}, severity={}",
                organizationId, resource.type(), resource.severity());
        var command = CreateIncidentCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var incident = incidentCommandService.handle(command);
        return ResponseEntityFromIncidentCommandResultAssembler.toResponseEntityFromCreateResult(
                incident,
                messageSource
        );
    }

    /**
     * Acknowledges an incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @param resource acknowledgement request resource
     * @return response entity containing the acknowledged incident resource
     */
    @Operation(
            summary = "Acknowledge an incident",
            description = "Moves an open incident to the acknowledged lifecycle state through the application service",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Incident acknowledgement request",
                    content = @Content(schema = @Schema(implementation = AcknowledgeIncidentResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident acknowledged",
                    content = @Content(schema = @Schema(implementation = IncidentResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid incident lifecycle transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{incidentId}/acknowledgements")
    public ResponseEntity<?> acknowledgeIncident(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId,
            @Valid @RequestBody AcknowledgeIncidentResource resource) {
        log.debug("POST /organizations/{}/incidents/{}/acknowledgements", organizationId, incidentId);
        var command = AcknowledgeIncidentCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                incidentId
        );
        var incident = incidentCommandService.handle(command);
        return ResponseEntityFromIncidentCommandResultAssembler.toResponseEntityFromLifecycleResult(
                incident,
                messageSource
        );
    }

    /**
     * Escalates an incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @param resource escalation request resource
     * @return response entity containing the escalated incident resource
     */
    @Operation(
            summary = "Escalate an incident",
            description = "Registers escalation fields for an open or acknowledged incident",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Incident escalation request",
                    content = @Content(schema = @Schema(implementation = EscalateIncidentResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident escalated",
                    content = @Content(schema = @Schema(implementation = IncidentResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid incident lifecycle transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{incidentId}/escalation")
    public ResponseEntity<?> escalateIncident(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId,
            @Valid @RequestBody EscalateIncidentResource resource) {
        log.debug("PATCH /organizations/{}/incidents/{}/escalation", organizationId, incidentId);
        var command = EscalateIncidentCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                incidentId
        );
        var incident = incidentCommandService.handle(command);
        return ResponseEntityFromIncidentCommandResultAssembler.toResponseEntityFromLifecycleResult(
                incident,
                messageSource
        );
    }

    /**
     * Registers corrective action for an incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @param resource corrective action request resource
     * @return response entity containing the updated incident resource
     */
    @Operation(
            summary = "Register incident corrective action",
            description = "Stores corrective action details for an open or acknowledged incident",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Incident corrective action request",
                    content = @Content(schema = @Schema(implementation = RegisterIncidentCorrectiveActionResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Corrective action registered",
                    content = @Content(schema = @Schema(implementation = IncidentResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid incident lifecycle transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{incidentId}/corrective-action")
    public ResponseEntity<?> registerIncidentCorrectiveAction(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId,
            @Valid @RequestBody RegisterIncidentCorrectiveActionResource resource) {
        log.debug("PATCH /organizations/{}/incidents/{}/corrective-action", organizationId, incidentId);
        var command = RegisterIncidentCorrectiveActionCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                incidentId
        );
        var incident = incidentCommandService.handle(command);
        return ResponseEntityFromIncidentCommandResultAssembler.toResponseEntityFromLifecycleResult(
                incident,
                messageSource
        );
    }

    /**
     * Resolves an incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @param resource resolution request resource
     * @return response entity containing the resolved incident resource
     */
    @Operation(
            summary = "Resolve an incident",
            description = "Moves an open or acknowledged incident to the resolved lifecycle state through the application service",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Incident resolution request",
                    content = @Content(schema = @Schema(implementation = ResolveIncidentResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Incident resolved",
                    content = @Content(schema = @Schema(implementation = IncidentResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid incident lifecycle transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{incidentId}/resolutions")
    public ResponseEntity<?> resolveIncident(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId,
            @Valid @RequestBody ResolveIncidentResource resource) {
        log.debug("POST /organizations/{}/incidents/{}/resolutions", organizationId, incidentId);
        var command = ResolveIncidentCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                incidentId
        );
        var incident = incidentCommandService.handle(command);
        return ResponseEntityFromIncidentCommandResultAssembler.toResponseEntityFromLifecycleResult(
                incident,
                messageSource
        );
    }

    /**
     * Generates an AI resolution plan for an active incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @return response entity containing the persisted pending AI resolution plan
     */
    @Operation(summary = "Generate an incident AI resolution plan",
            description = "Loads backend-owned incident context, requests structured AI guidance and persists a pending plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "AI resolution plan generated",
                    content = @Content(schema = @Schema(implementation = AiResolutionPlanResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Incident cannot receive generated plans in its current state",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "AI provider returned invalid structured output",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "AI provider is unavailable or disabled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "504", description = "AI provider timed out",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{incidentId}/ai-resolution-plans")
    public ResponseEntity<?> generateAiResolutionPlan(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId) {
        log.debug("POST /organizations/{}/incidents/{}/ai-resolution-plans", organizationId, incidentId);
        var plan = aiResolutionPlanCommandService.handle(
                new GenerateAiResolutionPlanCommand(organizationId, incidentId)
        );
        return ResponseEntityFromAiResolutionPlanCommandResultAssembler.toResponseEntityFromCreateResult(
                plan,
                messageSource
        );
    }

    /**
     * Approves an AI resolution plan and resolves its incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @param planId AI resolution plan identifier
     * @param resource approval request resource
     * @return response entity containing the approved AI resolution plan
     */
    @Operation(
            summary = "Approve an incident AI resolution plan",
            description = "Approves a pending AI plan, stores final operator notes and resolves the incident through backend lifecycle rules",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "AI resolution plan approval request",
                    content = @Content(schema = @Schema(implementation = ApproveAiResolutionPlanResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI resolution plan approved and incident resolved",
                    content = @Content(schema = @Schema(implementation = AiResolutionPlanResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid approval request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, incident, or AI plan not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Plan already decided or incident already resolved",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{incidentId}/ai-resolution-plans/{planId}/approvals")
    public ResponseEntity<?> approveAiResolutionPlan(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId,
            @Parameter(name = "planId", description = "AI resolution plan identifier", required = true)
            @PathVariable Long planId,
            @Valid @RequestBody ApproveAiResolutionPlanResource resource) {
        log.debug("POST /organizations/{}/incidents/{}/ai-resolution-plans/{}/approvals",
                organizationId, incidentId, planId);
        var command = ApproveAiResolutionPlanCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                incidentId,
                planId
        );
        var plan = aiResolutionPlanCommandService.handle(command);
        return ResponseEntityFromAiResolutionPlanCommandResultAssembler.toResponseEntityFromLifecycleResult(
                plan,
                messageSource
        );
    }

    /**
     * Rejects an AI resolution plan without changing incident status.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @param planId AI resolution plan identifier
     * @param resource rejection request resource
     * @return response entity containing the rejected AI resolution plan
     */
    @Operation(
            summary = "Reject an incident AI resolution plan",
            description = "Rejects a pending AI plan, stores operator audit metadata and leaves the incident lifecycle unchanged",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "AI resolution plan rejection request",
                    content = @Content(schema = @Schema(implementation = RejectAiResolutionPlanResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI resolution plan rejected",
                    content = @Content(schema = @Schema(implementation = AiResolutionPlanResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid rejection request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, incident, or AI plan not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Plan already approved or rejected",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{incidentId}/ai-resolution-plans/{planId}/rejections")
    public ResponseEntity<?> rejectAiResolutionPlan(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId,
            @Parameter(name = "planId", description = "AI resolution plan identifier", required = true)
            @PathVariable Long planId,
            @Valid @RequestBody RejectAiResolutionPlanResource resource) {
        log.debug("POST /organizations/{}/incidents/{}/ai-resolution-plans/{}/rejections",
                organizationId, incidentId, planId);
        var command = RejectAiResolutionPlanCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                incidentId,
                planId
        );
        var plan = aiResolutionPlanCommandService.handle(command);
        return ResponseEntityFromAiResolutionPlanCommandResultAssembler.toResponseEntityFromLifecycleResult(
                plan,
                messageSource
        );
    }

    /**
     * Gets AI resolution plan history for an incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @return response entity containing AI resolution plan resources
     */
    @Operation(summary = "Get incident AI resolution plan history",
            description = "Gets generated, approved, and rejected AI resolution plans scoped to the provided incident")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI resolution plan history found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AiResolutionPlanResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{incidentId}/ai-resolution-plans")
    public ResponseEntity<?> getAiResolutionPlansByIncidentId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId) {
        log.debug("GET /organizations/{}/incidents/{}/ai-resolution-plans", organizationId, incidentId);
        var plans = aiResolutionPlanQueryService.handle(
                new GetAiResolutionPlansByIncidentIdAndOrganizationIdQuery(organizationId, incidentId)
        );
        return ResponseEntityFromAiResolutionPlanQueryResultAssembler.toResponseEntityFromListResult(
                plans,
                messageSource
        );
    }

    /**
     * Gets notifications derived from one incident.
     *
     * @param organizationId organization identifier
     * @param incidentId incident identifier
     * @return response entity containing notification resources
     */
    @Operation(summary = "Get incident notifications",
            description = "Gets notification read models derived from the provided incident")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or incident not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{incidentId}/notifications")
    public ResponseEntity<?> getNotificationsByIncidentId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "incidentId", description = "Incident identifier", required = true)
            @PathVariable Long incidentId) {
        log.debug("GET /organizations/{}/incidents/{}/notifications", organizationId, incidentId);
        var notifications = notificationQueryService.handle(
                new GetNotificationsByIncidentIdAndOrganizationIdQuery(organizationId, incidentId)
        );
        return ResponseEntityFromNotificationQueryResultAssembler.toResponseEntityFromListResult(
                notifications,
                messageSource
        );
    }
}
