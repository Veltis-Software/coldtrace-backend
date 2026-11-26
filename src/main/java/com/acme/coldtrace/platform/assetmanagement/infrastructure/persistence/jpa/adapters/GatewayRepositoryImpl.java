package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.GatewayUuid;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.GatewayRepository;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.assemblers.GatewayPersistenceAssembler;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.repositories.GatewayPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed adapter for the gateway domain repository.
 *
 * @since 1.0
 */
@Repository
public class GatewayRepositoryImpl implements GatewayRepository {
    private final GatewayPersistenceRepository gatewayPersistenceRepository;

    public GatewayRepositoryImpl(GatewayPersistenceRepository gatewayPersistenceRepository) {
        this.gatewayPersistenceRepository = gatewayPersistenceRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Gateway> findAllByOrganizationId(Long organizationId) {
        return gatewayPersistenceRepository.findAllByOrganizationId(organizationId).stream()
                .map(GatewayPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Gateway> findByIdAndOrganizationId(Long id, Long organizationId) {
        return gatewayPersistenceRepository.findByIdAndOrganizationId(id, organizationId)
                .map(GatewayPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Gateway save(Gateway gateway) {
        if (gateway.getId() == null) {
            var entity = GatewayPersistenceAssembler.toPersistenceFromDomain(gateway);
            var savedEntity = gatewayPersistenceRepository.save(entity);
            return GatewayPersistenceAssembler.toDomainFromPersistence(savedEntity);
        }

        var entity = gatewayPersistenceRepository.findById(gateway.getId())
                .orElseGet(() -> GatewayPersistenceAssembler.toPersistenceFromDomain(gateway));
        GatewayPersistenceAssembler.copyDomainState(gateway, entity);
        var savedEntity = gatewayPersistenceRepository.save(entity);
        return GatewayPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndUuid(Long organizationId, String uuid) {
        return gatewayPersistenceRepository.existsByOrganizationIdAndUuid(organizationId, new GatewayUuid(uuid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, String uuid, Long id) {
        return gatewayPersistenceRepository.existsByOrganizationIdAndUuidAndIdNot(
                organizationId,
                new GatewayUuid(uuid),
                id
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        gatewayPersistenceRepository.deleteById(id);
        gatewayPersistenceRepository.flush();
    }
}
