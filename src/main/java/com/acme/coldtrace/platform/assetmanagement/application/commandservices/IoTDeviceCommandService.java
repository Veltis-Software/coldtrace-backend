package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.IoTDevice;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateIoTDeviceCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;

/**
 * Application service contract for IoT device command operations.
 *
 * @since 1.0
 */
public interface IoTDeviceCommandService {
    /**
     * Handles an IoT device creation request.
     *
     * @param command command containing device registration data
     * @return success with created device or failure with command error
     */
    Result<IoTDevice, IoTDeviceCommandFailure> handle(CreateIoTDeviceCommand command);

    /**
     * Handles an IoT device update request.
     *
     * @param command command containing replacement device data
     * @return success with updated device or failure with command error
     */
    Result<IoTDevice, IoTDeviceCommandFailure> handle(UpdateIoTDeviceCommand command);

    /**
     * Handles an IoT device deletion request.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with command error
     */
    Result<DeleteIoTDeviceCommand, IoTDeviceCommandFailure> handle(DeleteIoTDeviceCommand command);
}
