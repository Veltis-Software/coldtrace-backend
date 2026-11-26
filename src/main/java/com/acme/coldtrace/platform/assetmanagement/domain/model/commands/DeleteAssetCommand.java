package com.acme.coldtrace.platform.assetmanagement.domain.model.commands;

/**
 * Command for deleting an asset inside an organization.
 *
 * @param organizationId organization identifier that scopes the asset
 * @param assetId asset identifier to delete
 * @since 1.0
 */
public record DeleteAssetCommand(Long organizationId, Long assetId) {
    /**
     * Validates the identifiers needed to delete the asset.
     *
     * @throws IllegalArgumentException if any identifier is null or not positive
     */
    public DeleteAssetCommand {
        if (organizationId == null || organizationId <= 0) {
            throw new IllegalArgumentException("asset-management.asset.error.organizationId.invalid");
        }
        if (assetId == null || assetId <= 0) {
            throw new IllegalArgumentException("asset-management.asset.error.assetId.invalid");
        }
    }
}
