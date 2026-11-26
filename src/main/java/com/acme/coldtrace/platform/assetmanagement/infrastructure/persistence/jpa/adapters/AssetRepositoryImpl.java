package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.AssetUuid;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.AssetRepository;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.assemblers.AssetPersistenceAssembler;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.repositories.AssetPersistenceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed adapter for the asset domain repository.
 * <p>
 * The adapter shields application services from persistence entities and keeps
 * all mapping decisions inside the infrastructure layer. It also preserves JPA
 * auditing data during updates by copying domain state into the managed entity
 * already stored in the database.
 *
 * @since 1.0
 */
@Repository
public class AssetRepositoryImpl implements AssetRepository {
    private final AssetPersistenceRepository assetPersistenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AssetRepositoryImpl(
            AssetPersistenceRepository assetPersistenceRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.assetPersistenceRepository = assetPersistenceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Asset> findAllByOrganizationId(Long organizationId) {
        return assetPersistenceRepository.findAllByOrganizationId(organizationId).stream()
                .map(AssetPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Asset> findByIdAndOrganizationId(Long id, Long organizationId) {
        return assetPersistenceRepository.findByIdAndOrganizationId(id, organizationId)
                .map(AssetPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Asset save(Asset asset) {
        if (asset.getId() == null) {
            var entity = AssetPersistenceAssembler.toPersistenceFromDomain(asset);
            var savedEntity = assetPersistenceRepository.save(entity);
            var savedAsset = AssetPersistenceAssembler.toDomainFromPersistence(savedEntity);
            savedAsset.onCreated();
            savedAsset.domainEvents().forEach(eventPublisher::publishEvent);
            savedAsset.clearDomainEvents();
            return savedAsset;
        }

        var entity = assetPersistenceRepository.findById(asset.getId())
                .orElseGet(() -> AssetPersistenceAssembler.toPersistenceFromDomain(asset));
        AssetPersistenceAssembler.copyDomainState(asset, entity);
        var savedEntity = assetPersistenceRepository.save(entity);
        return AssetPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndUuid(Long organizationId, String uuid) {
        return assetPersistenceRepository.existsByOrganizationIdAndUuid(organizationId, new AssetUuid(uuid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, String uuid, Long id) {
        return assetPersistenceRepository.existsByOrganizationIdAndUuidAndIdNot(
                organizationId,
                new AssetUuid(uuid),
                id
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        assetPersistenceRepository.deleteById(id);
        assetPersistenceRepository.flush();
    }
}
