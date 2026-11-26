package com.acme.coldtrace.platform.assetmanagement.domain.model.commands;

/**
 * Command for deleting an IoT device inside an organization.
 *
 * @param organizationId organization identifier that scopes the IoT device
 * @param iotDeviceId IoT device identifier to delete
 * @since 1.0
 */
public record DeleteIoTDeviceCommand(Long organizationId, Long iotDeviceId) {
    /**
     * Validates the identifiers needed to delete the IoT device.
     *
     * @throws IllegalArgumentException if any identifier is null or not positive
     */
    public DeleteIoTDeviceCommand {
        if (organizationId == null || organizationId <= 0) {
            throw new IllegalArgumentException("asset-management.iot-device.error.organizationId.invalid");
        }
        if (iotDeviceId == null || iotDeviceId <= 0) {
            throw new IllegalArgumentException("asset-management.iot-device.error.iotDeviceId.invalid");
        }
    }
}
