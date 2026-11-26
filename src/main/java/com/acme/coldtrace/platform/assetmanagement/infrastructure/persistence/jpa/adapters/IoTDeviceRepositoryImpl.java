package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.adapters;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.IoTDevice;
import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.IoTDeviceUuid;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.IoTDeviceRepository;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.assemblers.IoTDevicePersistenceAssembler;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.repositories.IoTDevicePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed adapter for the IoT device domain repository.
 *
 * @since 1.0
 */
@Repository
public class IoTDeviceRepositoryImpl implements IoTDeviceRepository {
    private final IoTDevicePersistenceRepository iotDevicePersistenceRepository;

    public IoTDeviceRepositoryImpl(IoTDevicePersistenceRepository iotDevicePersistenceRepository) {
        this.iotDevicePersistenceRepository = iotDevicePersistenceRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IoTDevice> findAllByOrganizationId(Long organizationId) {
        return iotDevicePersistenceRepository.findAllByOrganizationId(organizationId).stream()
                .map(IoTDevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IoTDevice> findByIdAndOrganizationId(Long id, Long organizationId) {
        return iotDevicePersistenceRepository.findByIdAndOrganizationId(id, organizationId)
                .map(IoTDevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<IoTDevice> lockByIdAndOrganizationId(Long id, Long organizationId) {
        return iotDevicePersistenceRepository.lockByIdAndOrganizationId(id, organizationId)
                .map(IoTDevicePersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IoTDevice save(IoTDevice iotDevice) {
        if (iotDevice.getId() == null) {
            var entity = IoTDevicePersistenceAssembler.toPersistenceFromDomain(iotDevice);
            var savedEntity = iotDevicePersistenceRepository.save(entity);
            return IoTDevicePersistenceAssembler.toDomainFromPersistence(savedEntity);
        }

        var entity = iotDevicePersistenceRepository.findById(iotDevice.getId())
                .orElseGet(() -> IoTDevicePersistenceAssembler.toPersistenceFromDomain(iotDevice));
        IoTDevicePersistenceAssembler.copyDomainState(iotDevice, entity);
        var savedEntity = iotDevicePersistenceRepository.save(entity);
        return IoTDevicePersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndUuid(Long organizationId, String uuid) {
        return iotDevicePersistenceRepository.existsByOrganizationIdAndUuid(
                organizationId,
                new IoTDeviceUuid(uuid)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, String uuid, Long id) {
        return iotDevicePersistenceRepository.existsByOrganizationIdAndUuidAndIdNot(
                organizationId,
                new IoTDeviceUuid(uuid),
                id
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        iotDevicePersistenceRepository.deleteById(id);
        iotDevicePersistenceRepository.flush();
    }
}
