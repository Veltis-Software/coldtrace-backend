package com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.repositories;

import com.acme.coldtrace.platform.assetmanagement.domain.model.valueobjects.IoTDeviceUuid;
import com.acme.coldtrace.platform.assetmanagement.infrastructure.persistence.jpa.entities.IoTDevicePersistenceEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for IoT device persistence entities.
 *
 * @since 1.0
 */
@Repository
public interface IoTDevicePersistenceRepository extends JpaRepository<IoTDevicePersistenceEntity, Long> {
    /**
     * Finds IoT devices by organization.
     *
     * @param organizationId organization identifier
     * @return persistence entities for the organization
     */
    @EntityGraph(attributePaths = "measurementParameters")
    List<IoTDevicePersistenceEntity> findAllByOrganizationId(Long organizationId);

    /**
     * Finds one IoT device by id and organization.
     *
     * @param id IoT device identifier
     * @param organizationId organization identifier
     * @return persistence entity when found
     */
    @EntityGraph(attributePaths = "measurementParameters")
    Optional<IoTDevicePersistenceEntity> findByIdAndOrganizationId(Long id, Long organizationId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from IoTDevicePersistenceEntity d where d.id = :id and d.organizationId = :organizationId")
    Optional<IoTDevicePersistenceEntity> lockByIdAndOrganizationId(Long id, Long organizationId);

    /**
     * Checks whether an IoT device uuid already exists in an organization.
     *
     * @param organizationId organization identifier
     * @param uuid IoT device uuid value object
     * @return true when the uuid exists
     */
    boolean existsByOrganizationIdAndUuid(Long organizationId, IoTDeviceUuid uuid);

    /**
     * Checks whether another IoT device uses an IoT device uuid.
     *
     * @param organizationId organization identifier
     * @param uuid IoT device uuid value object
     * @param id IoT device identifier excluded from the search
     * @return true when another entity uses the uuid
     */
    boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, IoTDeviceUuid uuid, Long id);
}
