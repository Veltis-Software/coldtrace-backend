package com.acme.coldtrace.platform.assetmanagement.application.commandservices;

import com.acme.coldtrace.platform.assetmanagement.domain.model.aggregates.Gateway;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.CreateGatewayCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.DeleteGatewayCommand;
import com.acme.coldtrace.platform.assetmanagement.domain.model.commands.UpdateGatewayCommand;
import com.acme.coldtrace.platform.shared.application.result.Result;

/**
 * Application service contract for gateway command operations.
 *
 * @since 1.0
 */
public interface GatewayCommandService {
    /**
     * Handles gateway creation.
     *
     * @param command command containing gateway data
     * @return success with created gateway or failure with a gateway command error
     * @see CreateGatewayCommand
     */
    Result<Gateway, GatewayCommandFailure> handle(CreateGatewayCommand command);

    /**
     * Handles gateway update.
     *
     * @param command command containing updated gateway data
     * @return success with updated gateway or failure with a gateway command error
     * @see UpdateGatewayCommand
     */
    Result<Gateway, GatewayCommandFailure> handle(UpdateGatewayCommand command);

    /**
     * Handles gateway deletion.
     *
     * @param command command containing route-scoped deletion identifiers
     * @return success with the command or failure with a gateway command error
     * @see DeleteGatewayCommand
     */
    Result<DeleteGatewayCommand, GatewayCommandFailure> handle(DeleteGatewayCommand command);
}
