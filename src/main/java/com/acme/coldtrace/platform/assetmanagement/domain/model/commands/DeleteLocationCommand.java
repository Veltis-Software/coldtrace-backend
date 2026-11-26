package com.acme.coldtrace.platform.assetmanagement.domain.model.commands;

/**
 * Command for deleting a location inside an organization.
 *
 * @param organizationId organization identifier that scopes the location
 * @param locationId location identifier to delete
 * @since 1.0
 */
public record DeleteLocationCommand(Long organizationId, Long locationId) {
    /**
     * Validates the identifiers needed to delete the location.
     *
     * @throws IllegalArgumentException if any identifier is null or not positive
     */
    public DeleteLocationCommand {
        if (organizationId == null || organizationId <= 0) {
            throw new IllegalArgumentException("asset-management.location.error.organizationId.invalid");
        }
        if (locationId == null || locationId <= 0) {
            throw new IllegalArgumentException("asset-management.location.error.locationId.invalid");
        }
    }
}
