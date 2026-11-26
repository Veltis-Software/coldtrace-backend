package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Location;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.LocationName;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.LocationRepository;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.assemblers.LocationPersistenceAssembler;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.repositories.LocationPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed adapter for the location domain repository.
 *
 * @since 1.0
 */
@Repository
public class LocationRepositoryImpl implements LocationRepository {
    private final LocationPersistenceRepository locationPersistenceRepository;

    public LocationRepositoryImpl(LocationPersistenceRepository locationPersistenceRepository) {
        this.locationPersistenceRepository = locationPersistenceRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Location> findAllByOrganizationId(Long organizationId) {
        return locationPersistenceRepository.findAllByOrganizationId(organizationId).stream()
                .map(LocationPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Location> findByIdAndOrganizationId(Long id, Long organizationId) {
        return locationPersistenceRepository.findByIdAndOrganizationId(id, organizationId)
                .map(LocationPersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location save(Location location) {
        if (location.getId() == null) {
            var entity = LocationPersistenceAssembler.toPersistenceFromDomain(location);
            var savedEntity = locationPersistenceRepository.save(entity);
            return LocationPersistenceAssembler.toDomainFromPersistence(savedEntity);
        }

        var entity = locationPersistenceRepository.findById(location.getId())
                .orElseGet(() -> LocationPersistenceAssembler.toPersistenceFromDomain(location));
        LocationPersistenceAssembler.copyDomainState(location, entity);
        var savedEntity = locationPersistenceRepository.save(entity);
        return LocationPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndName(Long organizationId, String name) {
        return locationPersistenceRepository.existsByOrganizationIdAndName(organizationId, new LocationName(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndNameAndIdNot(Long organizationId, String name, Long id) {
        return locationPersistenceRepository.existsByOrganizationIdAndNameAndIdNot(
                organizationId,
                new LocationName(name),
                id
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        locationPersistenceRepository.deleteById(id);
        locationPersistenceRepository.flush();
    }
}
