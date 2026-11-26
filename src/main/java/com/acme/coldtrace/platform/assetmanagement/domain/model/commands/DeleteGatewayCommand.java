package com.acme.coldtrace.platform.assetmanagement.domain.model.commands;

/**
 * Command for deleting a gateway inside an organization.
 *
 * @param organizationId organization identifier that scopes the gateway
 * @param gatewayId gateway identifier to delete
 * @since 1.0
 */
public record DeleteGatewayCommand(Long organizationId, Long gatewayId) {
    /**
     * Validates the identifiers needed to delete the gateway.
     *
     * @throws IllegalArgumentException if any identifier is null or not positive
     */
    public DeleteGatewayCommand {
        if (organizationId == null || organizationId <= 0) {
            throw new IllegalArgumentException("asset-management.gateway.error.organizationId.invalid");
        }
        if (gatewayId == null || gatewayId <= 0) {
            throw new IllegalArgumentException("asset-management.gateway.error.gatewayId.invalid");
        }
    }
}
