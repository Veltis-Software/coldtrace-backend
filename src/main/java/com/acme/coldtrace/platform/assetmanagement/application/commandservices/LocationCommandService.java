package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Location;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateLocationCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteLocationCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateLocationCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;

/**
 * Application service contract for location command operations.
 *
 * @since 1.0
 */
public interface LocationCommandService {
    /**
     * Handles location creation.
     *
     * @param command command containing location data
     * @return success with created location or failure with a location command error
     * @see CreateLocationCommand
     */
    Result<Location, LocationCommandFailure> handle(CreateLocationCommand command);

    /**
     * Handles location update.
     *
     * @param command command containing updated location data
     * @return success with updated location or failure with a location command error
     * @see UpdateLocationCommand
     */
    Result<Location, LocationCommandFailure> handle(UpdateLocationCommand command);

    /**
     * Handles location deletion.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with a location command error
     * @see DeleteLocationCommand
     */
    Result<DeleteLocationCommand, LocationCommandFailure> handle(DeleteLocationCommand command);
}
