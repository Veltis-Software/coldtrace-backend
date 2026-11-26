package com.acme.coldtrace.platform.iam.application.commandservices;

import com.acme.coldtrace.platform.iam.domain.model.aggregates.User;
import com.acme.coldtrace.platform.iam.domain.model.commands.AssignUserRoleCommand;
import com.acme.coldtrace.platform.iam.domain.model.commands.CreateUserCommand;
import com.acme.coldtrace.platform.iam.domain.model.commands.DeleteUserCommand;
import com.acme.coldtrace.platform.iam.domain.model.commands.SignInCommand;
import com.acme.coldtrace.platform.shared.application.result.ApplicationError;
import com.acme.coldtrace.platform.shared.application.result.Result;

/**
 * Application service contract for user command operations.
 * It returns a Result so the interface layer can map business failures to
 * stable HTTP responses.
 *
 * @since 1.0
 */
public interface UserCommandService {
    /**
     * Handles user sign-in.
     *
     * @param command command containing credentials
     * @return success with authenticated user and token or standardized application error
     * @see SignInCommand
     */
    Result<AuthenticatedUserCommandResult, ApplicationError> handle(SignInCommand command);

    /**
     * Handles user creation.
     *
     * @param command command containing user identity and relationship data
     * @return success with created user or failure with a user command error
     * @throws IllegalArgumentException if the command contains invalid user data
     * @see CreateUserCommand
     */
    Result<User, UserCommandFailure> handle(CreateUserCommand command);

    /**
     * Handles assignment of a role to an existing organization user.
     *
     * @param command command containing organization, user and target role identifiers
     * @return success with updated user or failure with a user command error
     * @throws IllegalArgumentException if the command contains invalid identifiers
     * @see AssignUserRoleCommand
     */
    Result<User, UserCommandFailure> handle(AssignUserRoleCommand command);

    /**
     * Handles deletion of an existing organization user.
     *
     * @param command command containing organization and user identifiers
     * @return success with the command or failure with a user command error
     * @throws IllegalArgumentException if the command contains invalid identifiers
     * @see DeleteUserCommand
     */
    Result<DeleteUserCommand, UserCommandFailure> handle(DeleteUserCommand command);
}
