package com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.GatewayCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.application.commandservices.GatewayCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateGatewayCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteGatewayCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateGatewayCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.GatewayRepository;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.LocationRepository;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway.ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT;

/**
 * Application service implementation for gateway command operations.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class GatewayCommandServiceImpl implements GatewayCommandService {
    private static final String DUPLICATE_GATEWAY_CONSTRAINT = ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT;

    private final GatewayRepository gatewayRepository;
    private final LocationRepository locationRepository;
    private final OrganizationRepository organizationRepository;

    public GatewayCommandServiceImpl(
            GatewayRepository gatewayRepository,
            LocationRepository locationRepository,
            OrganizationRepository organizationRepository
    ) {
        this.gatewayRepository = gatewayRepository;
        this.locationRepository = locationRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * Handles creation of a gateway aggregate.
     *
     * @param command command containing gateway data
     * @return success with created gateway or failure with a gateway command error
     * @see CreateGatewayCommand
     */
    @Override
    @Transactional
    public Result<Gateway, GatewayCommandFailure> handle(CreateGatewayCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for gateway creation: organizationId={}", command.organizationId());
            return Result.failure(new GatewayCommandFailure.OrganizationNotFound());
        }
        if (locationRepository.findByIdAndOrganizationId(command.locationId(), command.organizationId()).isEmpty()) {
            log.warn("Location not found for gateway creation: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new GatewayCommandFailure.LocationNotFound());
        }
        if (gatewayRepository.existsByOrganizationIdAndUuid(command.organizationId(), command.uuid())) {
            log.warn("Duplicate gateway uuid detected: organizationId={}, uuid={}",
                    command.organizationId(), command.uuid());
            return Result.failure(new GatewayCommandFailure.DuplicateUuid());
        }
        try {
            var gateway = gatewayRepository.save(new Gateway(command));
            log.info("Gateway created: id={}, organizationId={}, uuid={}",
                    gateway.getId(), gateway.getOrganizationId(), gateway.getUuid());
            return Result.success(gateway);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateGatewayViolation(exception)) {
                log.warn("Duplicate gateway detected by constraint: organizationId={}, uuid={}",
                        command.organizationId(), command.uuid());
                return Result.failure(new GatewayCommandFailure.DuplicateUuid());
            }
            throw exception;
        }
    }

    /**
     * Handles update of a gateway aggregate.
     *
     * @param command command containing updated gateway data
     * @return success with updated gateway or failure with a gateway command error
     * @see UpdateGatewayCommand
     */
    @Override
    @Transactional
    public Result<Gateway, GatewayCommandFailure> handle(UpdateGatewayCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for gateway update: organizationId={}", command.organizationId());
            return Result.failure(new GatewayCommandFailure.OrganizationNotFound());
        }
        if (locationRepository.findByIdAndOrganizationId(command.locationId(), command.organizationId()).isEmpty()) {
            log.warn("Location not found for gateway update: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new GatewayCommandFailure.LocationNotFound());
        }
        var gateway = gatewayRepository.findByIdAndOrganizationId(command.gatewayId(), command.organizationId());
        if (gateway.isEmpty()) {
            log.warn("Gateway not found for update: organizationId={}, gatewayId={}",
                    command.organizationId(), command.gatewayId());
            return Result.failure(new GatewayCommandFailure.GatewayNotFound());
        }
        if (gatewayRepository.existsByOrganizationIdAndUuidAndIdNot(
                command.organizationId(), command.uuid(), command.gatewayId())) {
            log.warn("Duplicate gateway uuid detected for update: organizationId={}, gatewayId={}, uuid={}",
                    command.organizationId(), command.gatewayId(), command.uuid());
            return Result.failure(new GatewayCommandFailure.DuplicateUuid());
        }
        try {
            gateway.get().update(command);
            var updatedGateway = gatewayRepository.save(gateway.get());
            log.info("Gateway updated: id={}, organizationId={}, uuid={}",
                    updatedGateway.getId(), updatedGateway.getOrganizationId(), updatedGateway.getUuid());
            return Result.success(updatedGateway);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateGatewayViolation(exception)) {
                log.warn("Duplicate gateway detected by constraint during update: organizationId={}, gatewayId={}, uuid={}",
                        command.organizationId(), command.gatewayId(), command.uuid());
                return Result.failure(new GatewayCommandFailure.DuplicateUuid());
            }
            throw exception;
        }
    }

    /**
     * Handles deletion of a gateway aggregate.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with a gateway command error
     * @see DeleteGatewayCommand
     */
    @Override
    @Transactional
    public Result<DeleteGatewayCommand, GatewayCommandFailure> handle(DeleteGatewayCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for gateway deletion: organizationId={}", command.organizationId());
            return Result.failure(new GatewayCommandFailure.OrganizationNotFound());
        }
        if (gatewayRepository.findByIdAndOrganizationId(command.gatewayId(), command.organizationId()).isEmpty()) {
            log.warn("Gateway not found for deletion: organizationId={}, gatewayId={}",
                    command.organizationId(), command.gatewayId());
            return Result.failure(new GatewayCommandFailure.GatewayNotFound());
        }
        try {
            gatewayRepository.deleteById(command.gatewayId());
            log.info("Gateway deleted: id={}, organizationId={}", command.gatewayId(), command.organizationId());
            return Result.success(command);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Gateway deletion blocked by related records: organizationId={}, gatewayId={}",
                    command.organizationId(), command.gatewayId());
            return Result.failure(new GatewayCommandFailure.DeleteBlocked());
        }
    }

    private boolean isDuplicateGatewayViolation(DataIntegrityViolationException exception) {
        Throwable violationCause = exception;
        while (violationCause != null) {
            String message = violationCause.getMessage();
            if (message != null && message.contains(DUPLICATE_GATEWAY_CONSTRAINT)) {
                return true;
            }
            violationCause = violationCause.getCause();
        }
        return false;
    }
}
