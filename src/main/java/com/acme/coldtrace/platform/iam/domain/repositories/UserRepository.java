package com.acme.coldtrace.platform.iam.domain.repositories;

import com.acme.coldtrace.platform.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for user aggregates.
 *
 * @since 1.0
 */
public interface UserRepository {
    /**
     * Finds users linked to an organization.
     *
     * @param organizationId organization identifier
     * @return users for the organization
     */
    List<User> findAllByOrganizationId(Long organizationId);

    /**
     * Finds a user by its identifier and organization.
     * <p>
     * This method is used by organization-scoped commands to guarantee that a
     * user can only be modified through the organization route where the user
     * actually belongs.
     *
     * @param id user identifier
     * @param organizationId organization identifier
     * @return matching user when it belongs to the organization
     */
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    /**
     * Finds a user by its identifier.
     *
     * @param id user identifier
     * @return matching user when found
     */
    Optional<User> findById(Long id);

    /**
     * Finds a user by email.
     *
     * @param email user email
     * @return matching user when found
     */
    Optional<User> findByEmail(String email);

    /**
     * Persists a user aggregate.
     *
     * @param user user aggregate
     * @return persisted user rebuilt from persistence state
     */
    User save(User user);

    /**
     * Checks whether an email is already used by a user.
     *
     * @param email user email
     * @return true when the email exists
     */
    boolean existsByEmail(String email);

    /**
     * Deletes a user by identifier after the application service has checked
     * organization ownership.
     *
     * @param id user identifier
     */
    void deleteById(Long id);
}
