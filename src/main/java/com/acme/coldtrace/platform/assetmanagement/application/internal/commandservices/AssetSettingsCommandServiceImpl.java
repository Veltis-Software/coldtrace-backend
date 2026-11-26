package com.acme.coldtrace.platform.assetmanagement.application.internal.commandservices;

import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetSettingsCommandFailure;
import com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetSettingsCommandService;
import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.AssetSettings;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.SaveAssetSettingsCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.AssetRepository;
import com.acme.coldtrace.platform.assetmanagement.domain.repositories.AssetSettingsRepository;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementation for asset settings command operations.
 * <p>
 * The service enforces cross-aggregate rules: the organization must exist, and
 * asset-specific settings can only be saved for assets owned by that
 * organization. Persistence is idempotent so repeated PUT requests update the
 * existing settings record instead of creating duplicates.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class AssetSettingsCommandServiceImpl implements AssetSettingsCommandService {
    private final AssetSettingsRepository assetSettingsRepository;
    private final AssetRepository assetRepository;
    private final OrganizationRepository organizationRepository;
    private final com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetSettingsEvents events;

    public AssetSettingsCommandServiceImpl(
            AssetSettingsRepository assetSettingsRepository,
            AssetRepository assetRepository,
            OrganizationRepository organizationRepository,
            com.acme.coldtrace.platform.assetmanagement.application.commandservices.AssetSettingsEvents events
    ) {
        this.assetSettingsRepository = assetSettingsRepository;
        this.assetRepository = assetRepository;
        this.organizationRepository = organizationRepository;
        this.events = events;
    }

    /**
     * Handles idempotent persistence of asset settings.
     *
     * @param command command containing threshold and operational settings
     * @return success with saved settings or failure with a command error
     * @see SaveAssetSettingsCommand
     */
    @Override
    @Transactional
    public Result<AssetSettings, AssetSettingsCommandFailure> handle(SaveAssetSettingsCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for asset settings: organizationId={}", command.organizationId());
            return Result.failure(new AssetSettingsCommandFailure.OrganizationNotFound());
        }
        if (command.assetId() != null &&
                assetRepository.findByIdAndOrganizationId(command.assetId(), command.organizationId()).isEmpty()) {
            log.warn("Asset not found for asset settings: organizationId={}, assetId={}",
                    command.organizationId(), command.assetId());
            return Result.failure(new AssetSettingsCommandFailure.AssetNotFound());
        }

        var existingSettings = command.assetId() == null
                ? assetSettingsRepository.findDefaultByOrganizationId(command.organizationId())
                : assetSettingsRepository.findByOrganizationIdAndAssetId(command.organizationId(), command.assetId());
        var settings = existingSettings.orElseGet(() -> new AssetSettings(command));
        if (existingSettings.isPresent()) {
            settings.update(command);
        }
        var savedSettings = assetSettingsRepository.save(settings);
        events.changed(savedSettings);
        log.info("Asset settings saved: id={}, organizationId={}, assetId={}",
                savedSettings.getId(), savedSettings.getOrganizationId(), savedSettings.getAssetId());
        return Result.success(savedSettings);
    }
}
