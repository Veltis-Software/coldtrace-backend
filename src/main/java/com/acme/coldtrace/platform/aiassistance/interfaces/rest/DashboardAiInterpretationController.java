package com.acme.coldtrace.platform.aiassistance.interfaces.rest;

import com.acme.coldtrace.platform.aiassistance.application.commandservices.DashboardAiInterpretationCommandService;
import com.acme.coldtrace.platform.aiassistance.domain.model.commands.GenerateDashboardAiInterpretationCommand;
import com.acme.coldtrace.platform.aiassistance.interfaces.rest.resources.DashboardAiInterpretationResource;
import com.acme.coldtrace.platform.aiassistance.interfaces.rest.resources.GenerateDashboardAiInterpretationResource;
import com.acme.coldtrace.platform.aiassistance.interfaces.rest.transform.ResponseEntityFromDashboardAiInterpretationCommandResultAssembler;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing dashboard AI interpretation endpoints.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/dashboard", produces = APPLICATION_JSON_VALUE)
@Tag(name = "AI Assistance", description = "Endpoints for structured AI-assisted operational guidance")
public class DashboardAiInterpretationController {
    private final DashboardAiInterpretationCommandService dashboardAiInterpretationCommandService;
    private final MessageSource messageSource;

    public DashboardAiInterpretationController(
            DashboardAiInterpretationCommandService dashboardAiInterpretationCommandService,
            MessageSource messageSource
    ) {
        this.dashboardAiInterpretationCommandService = dashboardAiInterpretationCommandService;
        this.messageSource = messageSource;
    }

    /**
     * Generates an advisory AI interpretation for the operational dashboard.
     *
     * @param organizationId organization identifier
     * @param resource optional question request resource
     * @param acceptLanguageHeader optional HTTP language preference header
     * @return response entity containing the generated advisory interpretation
     */
    @Operation(
            summary = "Generate a dashboard AI interpretation",
            description = "Loads persisted dashboard evidence and returns a structured advisory interpretation without mutating operational data",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Optional dashboard question",
                    content = @Content(schema = @Schema(implementation = GenerateDashboardAiInterpretationResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI dashboard interpretation generated",
                    content = @Content(schema = @Schema(implementation = DashboardAiInterpretationResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier or question",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Dashboard context could not be prepared",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "AI provider returned invalid structured output",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "AI provider is unavailable or disabled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "504", description = "AI provider timed out",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/ai-interpretation")
    public ResponseEntity<?> generateDashboardAiInterpretation(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody(required = false) GenerateDashboardAiInterpretationResource resource,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguageHeader) {
        log.debug("POST /organizations/{}/dashboard/ai-interpretation", organizationId);
        var question = resource == null ? null : resource.question();
        var preferredLanguage = resource == null ? null : resource.preferredLanguage();
        var interpretation = dashboardAiInterpretationCommandService.handle(
                new GenerateDashboardAiInterpretationCommand(
                        organizationId,
                        question,
                        preferredLanguage,
                        acceptLanguageHeader
                )
        );
        return ResponseEntityFromDashboardAiInterpretationCommandResultAssembler.toResponseEntityFromGenerationResult(
                interpretation,
                messageSource
        );
    }
}
