package com.acme.coldtrace.platform.assetmanagement.interfaces.rest;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetSettingsCommandService;
import com.acme.coldtrace.platform.assetmanagement.application.queryservices.AssetSettingsQueryService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetAssetSettingsByOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetEffectiveAssetSettingsByAssetIdQuery;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.AssetSettingsResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.SaveAssetSettingsResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromAssetSettingsCommandResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromAssetSettingsQueryResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.SaveAssetSettingsCommandFromResourceAssembler;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing asset settings endpoints.
 * <p>
 * Asset settings are scoped by organization and can be stored either as
 * organization defaults or as asset-specific thresholds. The controller remains
 * thin by delegating validation and persistence decisions to command and query
 * services.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Asset Settings", description = "Endpoints for asset safety and telemetry settings")
public class AssetSettingsController {
    private final AssetSettingsCommandService assetSettingsCommandService;
    private final AssetSettingsQueryService assetSettingsQueryService;
    private final MessageSource messageSource;

    public AssetSettingsController(
            AssetSettingsCommandService assetSettingsCommandService,
            AssetSettingsQueryService assetSettingsQueryService,
            MessageSource messageSource
    ) {
        this.assetSettingsCommandService = assetSettingsCommandService;
        this.assetSettingsQueryService = assetSettingsQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets all asset settings owned by an organization.
     *
     * @param organizationId organization identifier
     * @return response entity containing settings resources
     */
    @Operation(summary = "Get asset settings by organization",
            description = "Gets default and asset-specific settings that belong to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Settings found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetSettingsResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/asset-settings")
    public ResponseEntity<?> getAssetSettingsByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/asset-settings", organizationId);
        var settings = assetSettingsQueryService.handle(new GetAssetSettingsByOrganizationIdQuery(organizationId));
        return ResponseEntityFromAssetSettingsQueryResultAssembler.toResponseEntityFromList(settings);
    }

    /**
     * Gets effective settings for one organization asset.
     *
     * @param organizationId organization identifier
     * @param assetId asset identifier
     * @return response entity containing the effective settings resource or failure detail
     */
    @Operation(summary = "Get effective asset settings",
            description = "Gets asset-specific settings or the organization default settings for one asset")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Settings found",
                    content = @Content(schema = @Schema(implementation = AssetSettingsResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Asset or settings not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/assets/{assetId}/settings")
    public ResponseEntity<?> getEffectiveAssetSettings(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "assetId", description = "Asset identifier", required = true)
            @PathVariable Long assetId) {
        log.debug("GET /organizations/{}/assets/{}/settings", organizationId, assetId);
        var settings = assetSettingsQueryService.handle(
                new GetEffectiveAssetSettingsByAssetIdQuery(organizationId, assetId)
        );
        return ResponseEntityFromAssetSettingsQueryResultAssembler.toResponseEntityFromAssetSettingsResult(
                settings,
                messageSource
        );
    }

    /**
     * Saves organization default settings.
     *
     * @param organizationId organization identifier
     * @param resource settings request resource
     * @return response entity containing saved default settings or failure detail
     */
    @Operation(
            summary = "Save default asset settings",
            description = "Creates or updates default safety and telemetry settings for an organization",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Asset settings request",
                    content = @Content(schema = @Schema(implementation = SaveAssetSettingsResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Default settings saved",
                    content = @Content(schema = @Schema(implementation = AssetSettingsResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/asset-settings/default")
    public ResponseEntity<?> saveDefaultAssetSettings(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody SaveAssetSettingsResource resource) {
        log.debug("PUT /organizations/{}/asset-settings/default", organizationId);
        var command = SaveAssetSettingsCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                null
        );
        var settings = assetSettingsCommandService.handle(command);
        return ResponseEntityFromAssetSettingsCommandResultAssembler.toResponseEntityFromSaveResult(
                settings,
                messageSource
        );
    }

    /**
     * Saves settings for one organization asset.
     *
     * @param organizationId organization identifier
     * @param assetId asset identifier
     * @param resource settings request resource
     * @return response entity containing saved asset settings or failure detail
     */
    @Operation(
            summary = "Save settings for an asset",
            description = "Creates or updates safety and telemetry settings for one organization asset",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Asset settings request",
                    content = @Content(schema = @Schema(implementation = SaveAssetSettingsResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset settings saved",
                    content = @Content(schema = @Schema(implementation = AssetSettingsResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/assets/{assetId}/settings")
    public ResponseEntity<?> saveAssetSettings(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "assetId", description = "Asset identifier", required = true)
            @PathVariable Long assetId,
            @Valid @RequestBody SaveAssetSettingsResource resource) {
        log.debug("PUT /organizations/{}/assets/{}/settings", organizationId, assetId);
        var command = SaveAssetSettingsCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                assetId
        );
        var settings = assetSettingsCommandService.handle(command);
        return ResponseEntityFromAssetSettingsCommandResultAssembler.toResponseEntityFromSaveResult(
                settings,
                messageSource
        );
    }
}
