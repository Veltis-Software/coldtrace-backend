package com.acme.coldtrace.platform.assetmanagement.domain.repositories;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.IoTDevice;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository contract for IoT device aggregates.
 * <p>
 * Application services depend on this abstraction so registration, assignment
 * and telemetry preparation rules remain independent from Spring Data JPA.
 *
 * @since 1.0
 */
public interface IoTDeviceRepository {
    /**
     * Finds all IoT devices owned by the provided organization.
     *
     * @param organizationId organization identifier
     * @return IoT devices registered for the organization
     */
    List<IoTDevice> findAllByOrganizationId(Long organizationId);

    /**
     * Finds one IoT device by identifier and organization.
     *
     * @param id IoT device identifier
     * @param organizationId organization identifier
     * @return IoT device when it exists inside the organization
     */
    Optional<IoTDevice> findByIdAndOrganizationId(Long id, Long organizationId);

    /** Serializes source association changes with legacy device writes. Requires a transaction. */
    Optional<IoTDevice> lockByIdAndOrganizationId(Long id, Long organizationId);

    /**
     * Persists an IoT device aggregate.
     *
     * @param iotDevice IoT device aggregate to create or update
     * @return persisted IoT device aggregate rebuilt from persistence state
     */
    IoTDevice save(IoTDevice iotDevice);

    /**
     * Checks whether the provided business uuid is already used in an organization.
     *
     * @param organizationId organization identifier
     * @param uuid IoT device business uuid
     * @return true when the uuid is already in use
     */
    boolean existsByOrganizationIdAndUuid(Long organizationId, String uuid);

    /**
     * Checks whether the provided business uuid is used by another IoT device.
     *
     * @param organizationId organization identifier
     * @param uuid IoT device business uuid
     * @param id IoT device identifier excluded from the search
     * @return true when another device uses the uuid
     */
    boolean existsByOrganizationIdAndUuidAndIdNot(Long organizationId, String uuid, Long id);

    /**
     * Deletes an IoT device by identifier after the application service has
     * checked organization ownership.
     *
     * @param id IoT device identifier
     */
    void deleteById(Long id);
}
