package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Asset;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateAssetCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteAssetCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateAssetCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;

/**
 * Application service contract for asset command use cases.
 * <p>
 * Command services are responsible for coordinating aggregate creation and
 * mutation with repository checks that cannot be enforced by the aggregate
 * alone, such as organization existence, location ownership and uniqueness
 * inside the organization scope.
 *
 * @since 1.0
 */
public interface AssetCommandService {
    /**
     * Handles the asset creation use case.
     *
     * @param command command containing normalized creation data
     * @return success with the created asset or a typed command failure
     * @see CreateAssetCommand
     */
    Result<Asset, AssetCommandFailure> handle(CreateAssetCommand command);

    /**
     * Handles the asset update use case.
     *
     * @param command command containing normalized update data
     * @return success with the updated asset or a typed command failure
     * @see UpdateAssetCommand
     */
    Result<Asset, AssetCommandFailure> handle(UpdateAssetCommand command);

    /**
     * Handles the asset deletion use case.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or a typed command failure
     * @see DeleteAssetCommand
     */
    Result<DeleteAssetCommand, AssetCommandFailure> handle(DeleteAssetCommand command);
}
