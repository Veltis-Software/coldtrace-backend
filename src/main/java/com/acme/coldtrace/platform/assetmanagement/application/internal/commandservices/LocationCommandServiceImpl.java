package com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.LocationCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.application.commandservices.LocationCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Location;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateLocationCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteLocationCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateLocationCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.LocationRepository;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Location.ORGANIZATION_ID_NAME_UNIQUE_CONSTRAINT;
import static com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade.ENTITLEMENT_LOCATIONS;

/**
 * Application service implementation for location command operations.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class LocationCommandServiceImpl implements LocationCommandService {
    private static final String DUPLICATE_LOCATION_CONSTRAINT = ORGANIZATION_ID_NAME_UNIQUE_CONSTRAINT;

    private final LocationRepository locationRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionBillingContextFacade subscriptionBillingContextFacade;

    public LocationCommandServiceImpl(
            LocationRepository locationRepository,
            OrganizationRepository organizationRepository,
            SubscriptionBillingContextFacade subscriptionBillingContextFacade
    ) {
        this.locationRepository = locationRepository;
        this.organizationRepository = organizationRepository;
        this.subscriptionBillingContextFacade = subscriptionBillingContextFacade;
    }

    /**
     * Handles creation of a location aggregate.
     *
     * @param command command containing location data
     * @return success with created location or failure with a location command error
     * @see CreateLocationCommand
     */
    @Override
    @Transactional
    public Result<Location, LocationCommandFailure> handle(CreateLocationCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for location creation: organizationId={}", command.organizationId());
            return Result.failure(new LocationCommandFailure.OrganizationNotFound());
        }
        if (locationRepository.existsByOrganizationIdAndName(command.organizationId(), command.name())) {
            log.warn("Duplicate location name detected: organizationId={}, name={}",
                    command.organizationId(), command.name());
            return Result.failure(new LocationCommandFailure.DuplicateName());
        }
        var entitlement = subscriptionBillingContextFacade.checkEntitlement(
                command.organizationId(),
                ENTITLEMENT_LOCATIONS
        );
        if (entitlement.isPresent() && !Boolean.TRUE.equals(entitlement.get().enabled())) {
            log.warn("Location creation blocked by plan limit: organizationId={}, entitlement={}",
                    command.organizationId(), ENTITLEMENT_LOCATIONS);
            return Result.failure(new LocationCommandFailure.PlanLimitExceeded(entitlement.get()));
        }
        try {
            var location = locationRepository.save(new Location(command));
            log.info("Location created: id={}, organizationId={}, name={}",
                    location.getId(), location.getOrganizationId(), location.getName());
            return Result.success(location);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLocationViolation(exception)) {
                log.warn("Duplicate location detected by constraint: organizationId={}, name={}",
                        command.organizationId(), command.name());
                return Result.failure(new LocationCommandFailure.DuplicateName());
            }
            throw exception;
        }
    }

    /**
     * Handles update of a location aggregate.
     *
     * @param command command containing updated location data
     * @return success with updated location or failure with a location command error
     * @see UpdateLocationCommand
     */
    @Override
    @Transactional
    public Result<Location, LocationCommandFailure> handle(UpdateLocationCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for location update: organizationId={}", command.organizationId());
            return Result.failure(new LocationCommandFailure.OrganizationNotFound());
        }
        var location = locationRepository.findByIdAndOrganizationId(command.locationId(), command.organizationId());
        if (location.isEmpty()) {
            log.warn("Location not found for update: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new LocationCommandFailure.LocationNotFound());
        }
        if (locationRepository.existsByOrganizationIdAndNameAndIdNot(
                command.organizationId(), command.name(), command.locationId())) {
            log.warn("Duplicate location name detected for update: organizationId={}, locationId={}, name={}",
                    command.organizationId(), command.locationId(), command.name());
            return Result.failure(new LocationCommandFailure.DuplicateName());
        }
        try {
            location.get().update(command);
            var updatedLocation = locationRepository.save(location.get());
            log.info("Location updated: id={}, organizationId={}, name={}",
                    updatedLocation.getId(), updatedLocation.getOrganizationId(), updatedLocation.getName());
            return Result.success(updatedLocation);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateLocationViolation(exception)) {
                log.warn("Duplicate location detected by constraint during update: organizationId={}, locationId={}, name={}",
                        command.organizationId(), command.locationId(), command.name());
                return Result.failure(new LocationCommandFailure.DuplicateName());
            }
            throw exception;
        }
    }

    /**
     * Handles deletion of a location aggregate.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with a location command error
     * @see DeleteLocationCommand
     */
    @Override
    @Transactional
    public Result<DeleteLocationCommand, LocationCommandFailure> handle(DeleteLocationCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for location deletion: organizationId={}", command.organizationId());
            return Result.failure(new LocationCommandFailure.OrganizationNotFound());
        }
        if (locationRepository.findByIdAndOrganizationId(command.locationId(), command.organizationId()).isEmpty()) {
            log.warn("Location not found for deletion: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new LocationCommandFailure.LocationNotFound());
        }
        try {
            locationRepository.deleteById(command.locationId());
            log.info("Location deleted: id={}, organizationId={}",
                    command.locationId(), command.organizationId());
            return Result.success(command);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Location deletion blocked by related records: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new LocationCommandFailure.DeleteBlocked());
        }
    }

    private boolean isDuplicateLocationViolation(DataIntegrityViolationException exception) {
        Throwable violationCause = exception;
        while (violationCause != null) {
            String message = violationCause.getMessage();
            if (message != null && message.contains(DUPLICATE_LOCATION_CONSTRAINT)) {
                return true;
            }
            violationCause = violationCause.getCause();
        }
        return false;
    }
}
