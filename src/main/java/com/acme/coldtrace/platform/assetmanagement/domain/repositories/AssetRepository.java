package com.acme.coldtrace.platform.assetmanagement.domain.repositories;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for asset aggregates.
 * <p>
 * Application services depend on this abstraction instead of a Spring Data JPA
 * repository. Infrastructure adapters are responsible for translating between
 * persistence entities and the pure domain aggregate.
 *
 * @since 1.0
 */
public interface AssetRepository {
    /**
     * Finds all assets owned by the provided organization.
     *
     * @param organizationId organization identifier
     * @return assets registered for the organization
     */
    List<Asset> findAllByOrganizationId(Long organizationId);

    /**
     * Finds one asset by identifier and organization.
     *
     * @param id asset identifier
     * @param organizationId organization identifier
     * @return asset when it exists in the organization
     */
    Optional<Asset> findByIdAndOrganizationId(Long id, Long organizationId);

    /**
     * Persists an asset aggregate.
     *
     * @param asset asset aggregate to create or update
     * @return persisted asset aggregate rebuilt from persistence state
     */
    Asset save(Asset asset);

    /**
     * Checks whether the provided business uuid is already used in an organization.
     *
     * @param organizationId organization identifier
     * @param uuid asset business uuid
     * @return true when the uuid is already in use
     */
    boolean existsByOrganizationIdAndUuid(Long organizationId, String uuid);

    /**
     * Checks whether the provided business uuid is used by another asset.
     *
     * @param organizationId organization identifier
     * @param uuid asset business uuid
     * @param id asset identifier excluded from the search
     * @return true when another asset uses the same uuid
     */
    boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, String uuid, Long id);

    /**
     * Deletes an asset by identifier after the application service has checked
     * organization ownership.
     *
     * @param id asset identifier
     */
    void deleteById(Long id);
}
