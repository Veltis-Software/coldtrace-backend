package com.acme.coldtrace.platform.assetmanagement.interfaces.rest;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteAssetCommand;
import com.acme.coldtrace.platform.assetmanagement.application.queryservices.AssetQueryService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetAssetByIdAndOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.domain.model.queries.GetAssetsByOrganizationIdQuery;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.AssetResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.CreateAssetResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.resources.UpdateAssetResource;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.CreateAssetCommandFromResourceAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromAssetCommandResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.ResponseEntityFromAssetQueryResultAssembler;
import com.acme.coldtrace.platform.assetmanagement.interfaces.rest.transform.UpdateAssetCommandFromResourceAssembler;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing asset endpoints.
 * <p>
 * Assets are organization-scoped resources and are placed in locations owned by
 * the same organization. The controller keeps the route shape consistent with
 * the other asset management APIs by nesting resources below
 * {@code /organizations/{organizationId}} and delegating all business decisions
 * to application command and query services.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/assets", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Assets", description = "Endpoints for cold-chain assets")
public class AssetsController {
    private final AssetCommandService assetCommandService;
    private final AssetQueryService assetQueryService;
    private final MessageSource messageSource;

    public AssetsController(
            AssetCommandService assetCommandService,
            AssetQueryService assetQueryService,
            MessageSource messageSource
    ) {
        this.assetCommandService = assetCommandService;
        this.assetQueryService = assetQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets assets that belong to an organization.
     *
     * @param organizationId organization identifier used to filter assets
     * @return response entity containing asset resources
     */
    @Operation(summary = "Get assets by organization",
            description = "Gets cold-chain assets that belong to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assets found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssetResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getAssetsByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/assets", organizationId);
        var assets = assetQueryService.handle(new GetAssetsByOrganizationIdQuery(organizationId));
        return ResponseEntityFromAssetQueryResultAssembler.toResponseEntityFromList(assets);
    }

    /**
     * Gets one asset by id.
     *
     * @param organizationId organization identifier
     * @param assetId asset identifier
     * @return response entity containing one asset resource
     */
    @Operation(summary = "Get asset by id",
            description = "Gets one cold-chain asset that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset found",
                    content = @Content(schema = @Schema(implementation = AssetResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{assetId}")
    public ResponseEntity<?> getAssetById(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "assetId", description = "Asset identifier", required = true)
            @PathVariable Long assetId) {
        log.debug("GET /organizations/{}/assets/{}", organizationId, assetId);
        var asset = assetQueryService.handle(
                new GetAssetByIdAndOrganizationIdQuery(organizationId, assetId)
        );
        if (asset.isEmpty()) {
            log.debug("Asset not found for organizationId={}, assetId={}", organizationId, assetId);
            return ResponseEntityFromAssetQueryResultAssembler.notFound(
                    messageSource,
                    "asset-management.asset.error.asset-not-found"
            );
        }
        return ResponseEntityFromAssetQueryResultAssembler.toResponseEntityFromAsset(asset.orElseThrow());
    }

    /**
     * Creates an asset.
     *
     * @param organizationId organization identifier
     * @param resource asset creation request resource
     * @return response entity containing the created asset resource
     */
    @Operation(
            summary = "Create an asset",
            description = "Creates a cold-chain asset for an organization location",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Asset creation request",
                    content = @Content(schema = @Schema(implementation = CreateAssetResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asset created",
                    content = @Content(schema = @Schema(implementation = AssetResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or location not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - asset uuid already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createAsset(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateAssetResource resource) {
        log.debug("POST /organizations/{}/assets - uuid={}", organizationId, resource.uuid());
        var command = CreateAssetCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var asset = assetCommandService.handle(command);
        return ResponseEntityFromAssetCommandResultAssembler.toResponseEntityFromCreateResult(
                asset,
                messageSource
        );
    }

    /**
     * Updates an asset.
     *
     * @param organizationId organization identifier
     * @param assetId asset identifier
     * @param resource asset update request resource
     * @return response entity containing the updated asset resource
     */
    @Operation(
            summary = "Update an asset",
            description = "Updates a cold-chain asset for an organization location",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Asset update request",
                    content = @Content(schema = @Schema(implementation = UpdateAssetResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset updated",
                    content = @Content(schema = @Schema(implementation = AssetResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, location or asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - asset uuid already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{assetId}")
    public ResponseEntity<?> updateAsset(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "assetId", description = "Asset identifier", required = true)
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateAssetResource resource) {
        log.debug("PUT /organizations/{}/assets/{} - uuid={}",
                organizationId, assetId, resource.uuid());
        var command = UpdateAssetCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                assetId
        );
        var asset = assetCommandService.handle(command);
        return ResponseEntityFromAssetCommandResultAssembler.toResponseEntityFromUpdateResult(
                asset,
                messageSource
        );
    }

    /**
     * Deletes an asset.
     *
     * @param organizationId organization identifier
     * @param assetId asset identifier
     * @return empty response on success or failure detail
     */
    @Operation(summary = "Delete an asset",
            description = "Deletes one cold-chain asset that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Asset deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Asset cannot be deleted because related data exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{assetId}")
    public ResponseEntity<?> deleteAsset(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "assetId", description = "Asset identifier", required = true)
            @PathVariable Long assetId) {
        log.debug("DELETE /organizations/{}/assets/{}", organizationId, assetId);
        var result = assetCommandService.handle(new DeleteAssetCommand(organizationId, assetId));
        return ResponseEntityFromAssetCommandResultAssembler.toResponseEntityFromDeleteResult(
                result,
                messageSource
        );
    }
}
