package com.acme.coldtrace.platform.assetmanagement.domain.repositories;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for gateway aggregates.
 * <p>
 * Gateways belong to an organization and are installed in an organization
 * location. Application services use this contract to keep those invariants
 * separate from persistence entity concerns.
 *
 * @since 1.0
 */
public interface GatewayRepository {
    /**
     * Finds all gateways registered for an organization.
     *
     * @param organizationId organization identifier
     * @return organization gateways
     */
    List<Gateway> findAllByOrganizationId(Long organizationId);

    /**
     * Finds one gateway by id and organization.
     *
     * @param id gateway identifier
     * @param organizationId organization identifier
     * @return gateway when found
     */
    Optional<Gateway> findByIdAndOrganizationId(Long id, Long organizationId);

    /**
     * Persists a gateway aggregate.
     *
     * @param gateway gateway aggregate to create or update
     * @return persisted gateway aggregate rebuilt from persistence state
     */
    Gateway save(Gateway gateway);

    /**
     * Checks whether a gateway uuid already exists in an organization.
     *
     * @param organizationId organization identifier
     * @param uuid gateway business uuid
     * @return true when the uuid is already used
     */
    boolean existsByOrganizationIdAndUuid(Long organizationId, String uuid);

    /**
     * Checks whether another gateway uses the provided uuid.
     *
     * @param organizationId organization identifier
     * @param uuid gateway business uuid
     * @param id gateway identifier excluded from the search
     * @return true when another gateway uses the uuid
     */
    boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, String uuid, Long id);

    /**
     * Deletes a gateway by identifier after the application service has checked
     * organization ownership.
     *
     * @param id gateway identifier
     */
    void deleteById(Long id);
}
