package com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateAssetCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteAssetCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateAssetCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.AssetRepository;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.LocationRepository;
import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset.ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT;
import static com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade.ENTITLEMENT_ASSETS;

/**
 * Application service implementation for asset command operations.
 * <p>
 * This service coordinates persistence-level checks that require repositories:
 * organization existence, location ownership and asset uuid uniqueness inside
 * the organization. The aggregate remains focused on its own state while this
 * service protects cross-aggregate invariants required by the REST use cases.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class AssetCommandServiceImpl implements AssetCommandService {
    private static final String DUPLICATE_ASSET_CONSTRAINT = ORGANIZATION_ID_UUID_UNIQUE_CONSTRAINT;

    private final AssetRepository assetRepository;
    private final LocationRepository locationRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionBillingContextFacade subscriptionBillingContextFacade;

    public AssetCommandServiceImpl(
            AssetRepository assetRepository,
            LocationRepository locationRepository,
            OrganizationRepository organizationRepository,
            SubscriptionBillingContextFacade subscriptionBillingContextFacade
    ) {
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.organizationRepository = organizationRepository;
        this.subscriptionBillingContextFacade = subscriptionBillingContextFacade;
    }

    /**
     * Handles creation of an asset aggregate.
     *
     * @param command command containing asset data
     * @return success with created asset or failure with an asset command error
     * @see CreateAssetCommand
     */
    @Override
    @Transactional
    public Result<Asset, AssetCommandFailure> handle(CreateAssetCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for asset creation: organizationId={}", command.organizationId());
            return Result.failure(new AssetCommandFailure.OrganizationNotFound());
        }
        if (locationRepository.findByIdAndOrganizationId(command.locationId(), command.organizationId()).isEmpty()) {
            log.warn("Location not found for asset creation: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new AssetCommandFailure.LocationNotFound());
        }
        if (assetRepository.existsByOrganizationIdAndUuid(command.organizationId(), command.uuid())) {
            log.warn("Duplicate asset uuid detected: organizationId={}, uuid={}",
                    command.organizationId(), command.uuid());
            return Result.failure(new AssetCommandFailure.DuplicateUuid());
        }
        var entitlement = subscriptionBillingContextFacade.checkEntitlement(
                command.organizationId(),
                ENTITLEMENT_ASSETS
        );
        if (entitlement.isPresent() && !Boolean.TRUE.equals(entitlement.get().enabled())) {
            log.warn("Asset creation blocked by plan limit: organizationId={}, entitlement={}",
                    command.organizationId(), ENTITLEMENT_ASSETS);
            return Result.failure(new AssetCommandFailure.PlanLimitExceeded(entitlement.get()));
        }
        try {
            var asset = assetRepository.save(new Asset(command));
            log.info("Asset created: id={}, organizationId={}, uuid={}",
                    asset.getId(), asset.getOrganizationId(), asset.getUuid());
            return Result.success(asset);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateAssetViolation(exception)) {
                log.warn("Duplicate asset detected by constraint: organizationId={}, uuid={}",
                        command.organizationId(), command.uuid());
                return Result.failure(new AssetCommandFailure.DuplicateUuid());
            }
            throw exception;
        }
    }

    /**
     * Handles update of an asset aggregate.
     *
     * @param command command containing updated asset data
     * @return success with updated asset or failure with an asset command error
     * @see UpdateAssetCommand
     */
    @Override
    @Transactional
    public Result<Asset, AssetCommandFailure> handle(UpdateAssetCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for asset update: organizationId={}", command.organizationId());
            return Result.failure(new AssetCommandFailure.OrganizationNotFound());
        }
        if (locationRepository.findByIdAndOrganizationId(command.locationId(), command.organizationId()).isEmpty()) {
            log.warn("Location not found for asset update: organizationId={}, locationId={}",
                    command.organizationId(), command.locationId());
            return Result.failure(new AssetCommandFailure.LocationNotFound());
        }
        var asset = assetRepository.findByIdAndOrganizationId(command.assetId(), command.organizationId());
        if (asset.isEmpty()) {
            log.warn("Asset not found for update: organizationId={}, assetId={}",
                    command.organizationId(), command.assetId());
            return Result.failure(new AssetCommandFailure.AssetNotFound());
        }
        if (assetRepository.existsByOrganizationIdAndUuidAndIdNot(
                command.organizationId(), command.uuid(), command.assetId())) {
            log.warn("Duplicate asset uuid detected for update: organizationId={}, assetId={}, uuid={}",
                    command.organizationId(), command.assetId(), command.uuid());
            return Result.failure(new AssetCommandFailure.DuplicateUuid());
        }
        try {
            asset.get().update(command);
            var updatedAsset = assetRepository.save(asset.get());
            log.info("Asset updated: id={}, organizationId={}, uuid={}",
                    updatedAsset.getId(), updatedAsset.getOrganizationId(), updatedAsset.getUuid());
            return Result.success(updatedAsset);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateAssetViolation(exception)) {
                log.warn("Duplicate asset detected by constraint during update: organizationId={}, assetId={}, uuid={}",
                        command.organizationId(), command.assetId(), command.uuid());
                return Result.failure(new AssetCommandFailure.DuplicateUuid());
            }
            throw exception;
        }
    }

    /**
     * Handles deletion of an asset aggregate.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with an asset command error
     * @see DeleteAssetCommand
     */
    @Override
    @Transactional
    public Result<DeleteAssetCommand, AssetCommandFailure> handle(DeleteAssetCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for asset deletion: organizationId={}", command.organizationId());
            return Result.failure(new AssetCommandFailure.OrganizationNotFound());
        }
        if (assetRepository.findByIdAndOrganizationId(command.assetId(), command.organizationId()).isEmpty()) {
            log.warn("Asset not found for deletion: organizationId={}, assetId={}",
                    command.organizationId(), command.assetId());
            return Result.failure(new AssetCommandFailure.AssetNotFound());
        }
        try {
            assetRepository.deleteById(command.assetId());
            log.info("Asset deleted: id={}, organizationId={}", command.assetId(), command.organizationId());
            return Result.success(command);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Asset deletion blocked by related records: organizationId={}, assetId={}",
                    command.organizationId(), command.assetId());
            return Result.failure(new AssetCommandFailure.DeleteBlocked());
        }
    }

    private boolean isDuplicateAssetViolation(DataIntegrityViolationException exception) {
        Throwable violationCause = exception;
        while (violationCause != null) {
            String message = violationCause.getMessage();
            if (message != null && message.contains(DUPLICATE_ASSET_CONSTRAINT)) {
                return true;
            }
            violationCause = violationCause.getCause();
        }
        return false;
    }
}
