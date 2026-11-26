package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;

/**
 * Failure types produced while executing asset command use cases.
 * <p>
 * The interface is sealed so the interface layer can translate every known
 * application failure into a deterministic HTTP status without relying on
 * exception messages. Each failure also exposes an i18n message key so REST
 * responses can be localized by the controller assemblers.
 *
 * @since 1.0
 */
public sealed interface AssetCommandFailure
        permits AssetCommandFailure.DuplicateUuid,
        AssetCommandFailure.OrganizationNotFound,
        AssetCommandFailure.LocationNotFound,
        AssetCommandFailure.AssetNotFound,
        AssetCommandFailure.DeleteBlocked,
        AssetCommandFailure.PlanLimitExceeded {
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
     * Failure raised when another asset already uses the requested uuid inside
     * the same organization boundary.
     */
    record DuplicateUuid() implements AssetCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.asset.error.uuid.duplicate";
        }
    }

    /**
     * Failure raised when the organization path parameter does not identify an
     * existing organization.
     */
    record OrganizationNotFound() implements AssetCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.asset.error.organization-not-found";
        }
    }

    /**
     * Failure raised when the provided location does not exist for the selected
     * organization. This protects the asset aggregate from being attached across
     * organization boundaries.
     */
    record LocationNotFound() implements AssetCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.asset.error.location-not-found";
        }
    }

    /**
     * Failure raised when an update targets an asset that does not belong to the
     * provided organization.
     */
    record AssetNotFound() implements AssetCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.asset.error.asset-not-found";
        }
    }

    /**
     * Failure raised when related records prevent deleting the asset.
     */
    record DeleteBlocked() implements AssetCommandFailure {
        @Override
        public String messageKey() {
            return "asset-management.asset.error.delete-blocked";
        }
    }

    /** Plan limit exceeded failure. */
    record PlanLimitExceeded(SubscriptionBillingContextFacade.EntitlementCheckSnapshot entitlement)
            implements AssetCommandFailure, PlanEntitlementFailure {
        @Override
        public String messageKey() {
            return "asset-management.asset.error.plan-limit-exceeded";
        }
    }
}
