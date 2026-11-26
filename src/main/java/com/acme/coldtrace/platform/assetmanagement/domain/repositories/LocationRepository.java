package com.acme.coldtrace.platform.assetmanagement.domain.repositories;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Location;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for location aggregates.
 * <p>
 * Locations are organization-scoped placement points for assets and gateways.
 * Application services use this contract to validate ownership and uniqueness
 * without depending on JPA persistence entities.
 *
 * @since 1.0
 */
public interface LocationRepository {
    /**
     * Finds all locations registered for an organization.
     *
     * @param organizationId organization identifier
     * @return organization locations
     */
    List<Location> findAllByOrganizationId(Long organizationId);

    /**
     * Finds one location by id and organization.
     *
     * @param id location identifier
     * @param organizationId organization identifier
     * @return location when found
     */
    Optional<Location> findByIdAndOrganizationId(Long id, Long organizationId);

    /**
     * Persists a location aggregate.
     *
     * @param location location aggregate to create or update
     * @return persisted location aggregate rebuilt from persistence state
     */
    Location save(Location location);

    /**
     * Checks whether a location name already exists in an organization.
     *
     * @param organizationId organization identifier
     * @param name location name
     * @return true when the name is already used
     */
    boolean existsByOrganizationIdAndName(Long organizationId, String name);

    /**
     * Checks whether another location uses the provided name.
     *
     * @param organizationId organization identifier
     * @param name location name
     * @param id location identifier excluded from the search
     * @return true when another location uses the same name
     */
    boolean existsByOrganizationIdAndNameAndIdNot(Long organizationId, String name, Long id);

    /**
     * Deletes a location by identifier after the application service has checked
     * organization ownership.
     *
     * @param id location identifier
     */
    void deleteById(Long id);
}
