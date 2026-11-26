package com.acme.coldtrace.platform.iam.application.commandservices;

import com.acme.coldtrace.platform.billing.interfaces.acl.PlanEntitlementFailure;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;

/**
 * Failure types for user command execution.
 *
 * @since 1.0
 */
public sealed interface UserCommandFailure
        permits UserCommandFailure.DuplicateEmail,
        UserCommandFailure.OrganizationNotFound,
        UserCommandFailure.UserNotFound,
        UserCommandFailure.RoleNotFound,
        UserCommandFailure.DeleteBlocked,
        UserCommandFailure.PlanLimitExceeded {
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
     * Duplicate email failure.
     */
    record DuplicateEmail() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "identity-access.user.error.duplicate-email";
        }
    }

    /**
     * Organization not found failure.
     */
    record OrganizationNotFound() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "identity-access.user.error.organization-not-found";
        }
    }

    /**
     * User not found failure.
     */
    record UserNotFound() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "identity-access.user.error.user-not-found";
        }
    }

    /**
     * Role not found failure.
     */
    record RoleNotFound() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "identity-access.user.error.role-not-found";
        }
    }

    /**
     * User deletion blocked by dependent records.
     */
    record DeleteBlocked() implements UserCommandFailure {
        @Override
        public String messageKey() {
            return "identity-access.user.error.delete-blocked";
        }
    }

    /** Plan limit exceeded failure. */
    record PlanLimitExceeded(SubscriptionBillingContextFacade.EntitlementCheckSnapshot entitlement)
            implements UserCommandFailure, PlanEntitlementFailure {
        @Override
        public String messageKey() {
            return "identity-access.user.error.plan-limit-exceeded";
        }
    }
}
