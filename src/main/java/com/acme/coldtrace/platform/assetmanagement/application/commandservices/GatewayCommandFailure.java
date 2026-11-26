package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

/**
 * Failure types for gateway command execution.
 *
 * @since 1.0
 */
public sealed interface GatewayCommandFailure
        permits GatewayCommandFailure.DuplicateUuid,
        GatewayCommandFailure.OrganizationNotFound,
        GatewayCommandFailure.LocationNotFound,
        GatewayCommandFailure.GatewayNotFound,
        GatewayCommandFailure.DeleteBlocked {
    /**
     * Returns the message key associated with the failure.
     *
     * @return message key to resolve through i18n
     */
    String messageKey();

    /**
     * Returns optional arguments for message interpolation.
     *
     * @return message interpolation arguments
     */
    default Object[] args() {
        return new Object[0];
    }

    /**
     * Duplicate gateway uuid failure.
     */
    record DuplicateUuid() implements GatewayCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.gateway.error.uuid.duplicate";
        }
    }

    /**
     * Organization not found failure.
     */
    record OrganizationNotFound() implements GatewayCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.gateway.error.organization-not-found";
        }
    }

    /**
     * Location not found failure.
     */
    record LocationNotFound() implements GatewayCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.gateway.error.location-not-found";
        }
    }

    /**
     * Gateway not found failure.
     */
    record GatewayNotFound() implements GatewayCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.gateway.error.gateway-not-found";
        }
    }

    /**
     * Gateway deletion blocked by dependent records.
     */
    record DeleteBlocked() implements GatewayCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.gateway.error.delete-blocked";
        }
    }
}
