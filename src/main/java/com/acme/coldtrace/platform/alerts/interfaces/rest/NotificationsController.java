package com.acme.coldtrace.platform.alerts.interfaces.rest;

import com.acme.coldtrace.platform.alerts.application.queryservices.NotificationQueryService;
import com.acme.coldtrace.platform.alerts.domain.model.queries.GetNotificationsByOrganizationIdQuery;
import com.acme.coldtrace.platform.alerts.interfaces.rest.resources.NotificationResource;
import com.acme.coldtrace.platform.alerts.interfaces.rest.transform.ResponseEntityFromNotificationQueryResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
 * REST controller exposing notification read model endpoints.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/notifications", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Endpoints for incident notification read models")
public class NotificationsController {
    private final NotificationQueryService notificationQueryService;
    private final MessageSource messageSource;

    public NotificationsController(
            NotificationQueryService notificationQueryService,
            MessageSource messageSource
    ) {
        this.notificationQueryService = notificationQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets incident notifications that belong to an organization.
     *
     * @param organizationId organization identifier used to filter notifications
     * @return response entity containing notification resources
     */
    @Operation(summary = "Get notifications by organization",
            description = "Gets notification read models derived from incident lifecycle events")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getNotificationsByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/notifications", organizationId);
        var notifications = notificationQueryService.handle(new GetNotificationsByOrganizationIdQuery(organizationId));
        return ResponseEntityFromNotificationQueryResultAssembler.toResponseEntityFromListResult(
                notifications,
                messageSource
        );
    }
}
