package com.acme.coldtrace.platform.iam.domain.model.commands;

/**
 * Command for deleting a user inside an organization.
 *
 * @param organizationId organization identifier that scopes the user
 * @param userId user identifier to delete
 * @since 1.0
 */
public record DeleteUserCommand(Long organizationId, Long userId) {
    /**
     * Validates the identifiers needed to delete the user.
     *
     * @throws IllegalArgumentException if any identifier is null or not positive
     */
    public DeleteUserCommand {
        if (organizationId == null || organizationId <= 0) {
            throw new IllegalArgumentException("identity-access.user.error.organizationId.invalid");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("identity-access.user.error.userId.invalid");
        }
    }
}
