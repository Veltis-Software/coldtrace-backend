package com.acme.coldtrace.platform.iam.application.internal.commandservices;

import com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade;
import com.acme.coldtrace.platform.iam.application.commandservices.AuthenticatedUserCommandResult;
import com.acme.coldtrace.platform.iam.application.commandservices.UserCommandFailure;
import com.acme.coldtrace.platform.iam.application.commandservices.UserCommandService;
import com.acme.coldtrace.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.acme.coldtrace.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.acme.coldtrace.platform.iam.domain.model.aggregates.User;
import com.acme.coldtrace.platform.iam.domain.model.commands.AssignUserRoleCommand;
import com.acme.coldtrace.platform.iam.domain.model.commands.CreateUserCommand;
import com.acme.coldtrace.platform.iam.domain.model.commands.DeleteUserCommand;
import com.acme.coldtrace.platform.iam.domain.model.commands.SignInCommand;
import com.acme.coldtrace.platform.iam.domain.repositories.OrganizationRepository;
import com.acme.coldtrace.platform.iam.domain.repositories.RoleRepository;
import com.acme.coldtrace.platform.iam.domain.repositories.UserRepository;
import com.acme.coldtrace.platform.shared.application.result.ApplicationError;
import com.acme.coldtrace.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.acme.coldtrace.platform.billing.interfaces.acl.SubscriptionBillingContextFacade.ENTITLEMENT_USERS;

/**
 * Application service implementation for user command operations.
 * It enforces application-level creation rules such as unique email and valid
 * organization-role references before persisting the user aggregate.
 *
 * @since 1.0
 */
@Slf4j
@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final SubscriptionBillingContextFacade subscriptionBillingContextFacade;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            HashingService hashingService,
            TokenService tokenService,
            SubscriptionBillingContextFacade subscriptionBillingContextFacade
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.subscriptionBillingContextFacade = subscriptionBillingContextFacade;
    }

    /**
     * Handles authentication for an existing user.
     *
     * @param command command containing credentials
     * @return authenticated user with token or validation error
     * @see SignInCommand
     */
    @Override
    @Transactional(readOnly = true)
    public Result<AuthenticatedUserCommandResult, ApplicationError> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty()) {
            log.warn("Invalid sign-in credentials for email: {}", command.email());
            return invalidCredentials();
        }

        var authenticatedUser = user.get();
        if (authenticatedUser.getPasswordHash() == null ||
                authenticatedUser.getPasswordHash().isBlank() ||
                !hashingService.matches(command.password(), authenticatedUser.getPasswordHash())) {
            log.warn("Invalid sign-in credentials for userId={}, email={}",
                    authenticatedUser.getId(), authenticatedUser.getEmail());
            return invalidCredentials();
        }

        var token = tokenService.generateToken(authenticatedUser.getEmail());
        log.info("User authenticated: id={}, email={}", authenticatedUser.getId(), authenticatedUser.getEmail());
        return Result.success(new AuthenticatedUserCommandResult(authenticatedUser, token));
    }

    /**
     * Handles creation of a user aggregate.
     *
     * @param command command containing user data and references
     * @return success with the created user or failure with a command failure type
     * @throws DataIntegrityViolationException if an unexpected persistence constraint is violated
     * @see CreateUserCommand
     */
    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            log.warn("Duplicate user email detected: {}", command.email());
            return Result.failure(new UserCommandFailure.DuplicateEmail());
        }
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for user creation: organizationId={}", command.organizationId());
            return Result.failure(new UserCommandFailure.OrganizationNotFound());
        }
        if (!roleRepository.existsById(command.roleId())) {
            log.warn("Role not found for user creation: roleId={}", command.roleId());
            return Result.failure(new UserCommandFailure.RoleNotFound());
        }
        var entitlement = subscriptionBillingContextFacade.checkEntitlement(
                command.organizationId(),
                ENTITLEMENT_USERS
        );
        if (entitlement.isPresent() && !Boolean.TRUE.equals(entitlement.get().enabled())) {
            log.warn("User creation blocked by plan limit: organizationId={}, entitlement={}",
                    command.organizationId(), ENTITLEMENT_USERS);
            return Result.failure(new UserCommandFailure.PlanLimitExceeded(entitlement.get()));
        }

        try {
            var passwordHash = hashingService.encode(command.password());
            var user = userRepository.save(new User(command, passwordHash));
            log.info("User created: id={}, email={}, organizationId={}, roleId={}",
                    user.getId(), user.getEmail(), user.getOrganizationId(), user.getRoleId());
            return Result.success(user);
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByEmail(command.email())) {
                log.warn("Duplicate user email detected by constraint: {}", command.email());
                return Result.failure(new UserCommandFailure.DuplicateEmail());
            }
            throw exception;
        }
    }

    /**
     * Handles the assignment of a role to an existing user.
     * <p>
     * The operation is intentionally scoped by organization. A user found in
     * another organization is treated as not found for this route, which keeps
     * the REST contract aligned with organization-owned user management.
     *
     * @param command command containing organization, user and target role identifiers
     * @return success with the updated user or failure with a command failure type
     * @see AssignUserRoleCommand
     */
    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(AssignUserRoleCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for user role assignment: organizationId={}", command.organizationId());
            return Result.failure(new UserCommandFailure.OrganizationNotFound());
        }
        var user = userRepository.findByIdAndOrganizationId(command.userId(), command.organizationId());
        if (user.isEmpty()) {
            log.warn("User not found for role assignment: organizationId={}, userId={}",
                    command.organizationId(), command.userId());
            return Result.failure(new UserCommandFailure.UserNotFound());
        }
        if (!roleRepository.existsById(command.roleId())) {
            log.warn("Role not found for user role assignment: roleId={}", command.roleId());
            return Result.failure(new UserCommandFailure.RoleNotFound());
        }

        var userToUpdate = user.get();
        userToUpdate.assignRole(command);
        var updatedUser = userRepository.save(userToUpdate);
        log.info("User role assigned: userId={}, organizationId={}, roleId={}",
                updatedUser.getId(), updatedUser.getOrganizationId(), updatedUser.getRoleId());
        return Result.success(updatedUser);
    }

    /**
     * Handles deletion of an organization user.
     *
     * @param command command containing organization and user identifiers
     * @return success with the command or failure with a command failure type
     * @see DeleteUserCommand
     */
    @Override
    @Transactional
    public Result<DeleteUserCommand, UserCommandFailure> handle(DeleteUserCommand command) {
        if (!organizationRepository.existsById(command.organizationId())) {
            log.warn("Organization not found for user deletion: organizationId={}", command.organizationId());
            return Result.failure(new UserCommandFailure.OrganizationNotFound());
        }
        if (userRepository.findByIdAndOrganizationId(command.userId(), command.organizationId()).isEmpty()) {
            log.warn("User not found for deletion: organizationId={}, userId={}",
                    command.organizationId(), command.userId());
            return Result.failure(new UserCommandFailure.UserNotFound());
        }
        try {
            userRepository.deleteById(command.userId());
            log.info("User deleted: id={}, organizationId={}", command.userId(), command.organizationId());
            return Result.success(command);
        } catch (DataIntegrityViolationException exception) {
            log.warn("User deletion blocked by related records: organizationId={}, userId={}",
                    command.organizationId(), command.userId());
            return Result.failure(new UserCommandFailure.DeleteBlocked());
        }
    }

    private Result<AuthenticatedUserCommandResult, ApplicationError> invalidCredentials() {
        return Result.failure(ApplicationError.invalidCredentials(
                "identity-access.authentication.error.invalid-credentials"
        ));
    }
}
