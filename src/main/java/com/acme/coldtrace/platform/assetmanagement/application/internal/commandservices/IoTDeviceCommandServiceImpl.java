package com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.IoTDeviceCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.application.commandservices.IoTDeviceCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.IoTDevice;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateIoTDeviceCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.AssetRepository;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.GatewayRepository;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.IoTDeviceRepository;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.IoTDevice.ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT;
import static com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade.ENTITLEMENT_IOT_DEVICES;

/**
 * Application service implementation for IoT device command operations.
 * <p>
 * This service enforces organization ownership and location compatibility before
 * creating or updating the device aggregate. A device can remain connected to a
 * gateway without an asset assignment, but once an asset is provided both the
 * gateway and asset must belong to the same organization and location.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class IoTDeviceCommandServiceImpl implements IoTDeviceCommandService {
    private static final String DUPLICATE_IOT_DEVICE_CONSTRAINT = ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT;

    private final IoTDeviceRepository iotDeviceRepository;
    private final GatewayRepository gatewayRepository;
    private final AssetRepository assetRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionBillingContextFacade subscriptionBillingContextFacade;
    private final com.acme.coldtrace.platform.assetmanagement.domain.repositories.SourceConfigurationRepository sourceConfigurations;

    public IoTDeviceCommandServiceImpl(
            IoTDeviceRepository iotDeviceRepository,
            GatewayRepository gatewayRepository,
            AssetRepository assetRepository,
            OrganizationRepository organizationRepository,
            SubscriptionBillingContextFacade subscriptionBillingContextFacade,
            com.acme.coldtrace.platform.assetmanagement.domain.repositories.SourceConfigurationRepository sourceConfigurations
    ) {
        this.iotDeviceRepository = iotDeviceRepository;
        this.gatewayRepository = gatewayRepository;
        this.assetRepository = assetRepository;
        this.organizationRepository = organizationRepository;
        this.subscriptionBillingContextFacade = subscriptionBillingContextFacade;
        this.sourceConfigurations = sourceConfigurations;
    }

    /**
     * Handles creation of an IoT device aggregate.
     *
     * @param command command containing device registration data
     * @return success with created device or failure with command error
     * @see CreateIoTDeviceCommand
     */
    @Override
    @Transactional
    public Result<IoTDevice, IoTDeviceCommandFailure> handle(CreateIoTDeviceCommand command) {
        var validation = validateReferences(command.organizationId(), command.gatewayId(), command.assetId());
        if (validation.isFailure()) {
            return Result.failure(validation.failure().orElseThrow());
        }
        if (iotDeviceRepository.existsByOrganizationIdAndUuid(command.organizationId(), command.uuid())) {
            log.warn("Duplicate IoT device uuid detected: organizationId={}, uuid={}",
                    command.organizationId(), command.uuid());
            return Result.failure(new IoTDeviceCommandFailure.DuplicateUuid());
        }
        var entitlement = subscriptionBillingContextFacade.checkEntitlement(
                command.organizationId(),
                ENTITLEMENT_IOT_DEVICES
        );
        if (entitlement.isPresent() && !Boolean.TRUE.equals(entitlement.get().enabled())) {
            log.warn("IoT device creation blocked by plan limit: organizationId={}, entitlement={}",
                    command.organizationId(), ENTITLEMENT_IOT_DEVICES);
            return Result.failure(new IoTDeviceCommandFailure.PlanLimitExceeded(entitlement.get()));
        }
        try {
            var device = iotDeviceRepository.save(new IoTDevice(command));
            log.info("IoT device created: id={}, organizationId={}, uuid={}",
                    device.getId(), device.getOrganizationId(), device.getUuid());
            return Result.success(device);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateIoTDeviceViolation(exception)) {
                return Result.failure(new IoTDeviceCommandFailure.DuplicateUuid());
            }
            throw exception;
        }
    }

    /**
     * Handles update of an IoT device aggregate.
     *
     * @param command command containing updated device data
     * @return success with updated device or failure with command error
     * @see UpdateIoTDeviceCommand
     */
    @Override
    @Transactional
    public Result<IoTDevice, IoTDeviceCommandFailure> handle(UpdateIoTDeviceCommand command) {
        var validation = validateReferences(command.organizationId(), command.gatewayId(), command.assetId());
        if (validation.isFailure()) {
            return Result.failure(validation.failure().orElseThrow());
        }
        var device = iotDeviceRepository.lockByIdAndOrganizationId(command.iotDeviceId(), command.organizationId());
        if (device.isEmpty()) {
            log.warn("IoT device not found for update: organizationId={}, iotDeviceId={}",
                    command.organizationId(), command.iotDeviceId());
            return Result.failure(new IoTDeviceCommandFailure.IoTDeviceNotFound());
        }
        if (sourceConfigurations.find(command.organizationId(), command.iotDeviceId()).isPresent()
                && (!java.util.Objects.equals(device.get().getAssetId(), command.assetId())
                || !java.util.Objects.equals(device.get().getGatewayId(), command.gatewayId())
                || !device.get().getUuid().equals(command.uuid()))) {
            return Result.failure(new IoTDeviceCommandFailure.ManagedSourceConfiguration());
        }
        if (iotDeviceRepository.existsByOrganizationIdAndUuidAndIdNot(
                command.organizationId(), command.uuid(), command.iotDeviceId())) {
            return Result.failure(new IoTDeviceCommandFailure.DuplicateUuid());
        }
        try {
            device.get().update(command);
            var updatedDevice = iotDeviceRepository.save(device.get());
            log.info("IoT device updated: id={}, organizationId={}, uuid={}",
                    updatedDevice.getId(), updatedDevice.getOrganizationId(), updatedDevice.getUuid());
            return Result.success(updatedDevice);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateIoTDeviceViolation(exception)) {
                return Result.failure(new IoTDeviceCommandFailure.DuplicateUuid());
            }
            throw exception;
        }
    }

    /**
     * Handles deletion of an IoT device aggregate.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with command error
     * @see DeleteIoTDeviceCommand
     */
    @Override
    @Transactional
    public Result<DeleteIoTDeviceCommand, IoTDeviceCommandFailure> handle(DeleteIoTDeviceCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for IoT device deletion: organizationId={}", command.organizationId());
            return Result.failure(new IoTDeviceCommandFailure.OrganizationNotFound());
        }
        if (iotDeviceRepository.lockByIdAndOrganizationId(command.iotDeviceId(), command.organizationId()).isEmpty()) {
            log.warn("IoT device not found for deletion: organizationId={}, iotDeviceId={}",
                    command.organizationId(), command.iotDeviceId());
            return Result.failure(new IoTDeviceCommandFailure.IoTDeviceNotFound());
        }
        if (sourceConfigurations.find(command.organizationId(), command.iotDeviceId()).isPresent()) {
            return Result.failure(new IoTDeviceCommandFailure.ManagedSourceConfiguration());
        }
        try {
            iotDeviceRepository.deleteById(command.iotDeviceId());
            log.info("IoT device deleted: id={}, organizationId={}",
                    command.iotDeviceId(), command.organizationId());
            return Result.success(command);
        } catch (DataIntegrityViolationException exception) {
            log.warn("IoT device deletion blocked by related records: organizationId={}, iotDeviceId={}",
                    command.organizationId(), command.iotDeviceId());
            return Result.failure(new IoTDeviceCommandFailure.DeleteBlocked());
        }
    }

    private Result<Gateway, IoTDeviceCommandFailure> validateReferences(
            Long organizationId,
            Long gatewayId,
            Long assetId
    ) {
        if (!organizationRepository.existsById(organizationId)) {
            return Result.failure(new IoTDeviceCommandFailure.OrganizationNotFound());
        }
        var gateway = gatewayRepository.findByIdAndOrganizationId(gatewayId, organizationId);
        if (gateway.isEmpty()) {
            return Result.failure(new IoTDeviceCommandFailure.GatewayNotFound());
        }
        Optional<Asset> asset = assetId == null
                ? Optional.empty()
                : assetRepository.findByIdAndOrganizationId(assetId, organizationId);
        if (assetId != null && asset.isEmpty()) {
            return Result.failure(new IoTDeviceCommandFailure.AssetNotFound());
        }
        if (asset.isPresent() && !asset.get().getLocationId().equals(gateway.get().getLocationId())) {
            return Result.failure(new IoTDeviceCommandFailure.IncompatibleAssetLocation());
        }
        return Result.success(gateway.get());
    }

    private boolean isDuplicateIoTDeviceViolation(DataIntegrityViolationException exception) {
        Throwable violationCause = exception;
        while (violationCause != null) {
            String message = violationCause.getMessage();
            if (message != null && message.contains(DUPLICATE_IOT_DEVICE_CONSTRAINT)) {
                return true;
            }
            violationCause = violationCause.getCause();
        }
        return false;
    }
}
