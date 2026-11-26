package com.acme.coldtrace.platform.iam.interfaces.rest;

import com.acme.coldtrace.platform.iam.application.commandservices.UserCommandService;
import com.acme.coldtrace.platform.iam.domain.model.commands.DeleteUserCommand;
import com.acme.coldtrace.platform.iam.application.queryservices.UserQueryService;
import com.acme.coldtrace.platform.iam.domain.model.queries.GetUsersByOrganizationIdQuery;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.AssignUserRoleResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.CreateUserResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.UserResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.AssignUserRoleCommandFromResourceAssembler;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.CreateUserCommandFromResourceAssembler;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.ResponseEntityFromUserCommandResultAssembler;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.ResponseEntityFromUserQueryResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing user endpoints.
 * It translates HTTP requests into user commands and queries for IAM.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/organizations/{organizationId}/users", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "Endpoints for users")
public class UsersController {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final MessageSource messageSource;

    public UsersController(
            UserCommandService userCommandService,
            UserQueryService userQueryService,
            MessageSource messageSource
    ) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.messageSource = messageSource;
    }

    /**
     * Gets users that belong to an organization.
     *
     * @param organizationId organization identifier used to filter users
     * @return response entity containing user resources
     */
    @Operation(summary = "Get users by organization", description = "Gets users that belong to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserResource.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid organization identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<?> getUsersByOrganizationId(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId) {
        log.debug("GET /organizations/{}/users", organizationId);
        var users = userQueryService.handle(new GetUsersByOrganizationIdQuery(organizationId));
        return ResponseEntityFromUserQueryResultAssembler.toResponseEntityFromList(users, messageSource);
    }

    /**
     * Creates a user.
     *
     * @param resource user creation request resource
     * @return response entity containing the created user resource or failure detail
     */
    @Operation(
            summary = "Create a user",
            description = "Creates a user linked to an organization and role",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "User creation request",
                    content = @Content(schema = @Schema(implementation = CreateUserResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - email already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or role not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createUser(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateUserResource resource) {
        log.debug("POST /organizations/{}/users - email={}, roleId={}",
                organizationId, resource.email(), resource.roleId());
        var command = CreateUserCommandFromResourceAssembler.toCommandFromResource(resource, organizationId);
        var user = userCommandService.handle(command);
        return ResponseEntityFromUserCommandResultAssembler.toResponseEntityFromResult(user, messageSource);
    }

    /**
     * Assigns a role to an existing organization user.
     * <p>
     * The endpoint is organization-scoped, so the application service verifies
     * that the user belongs to the organization before applying the new role.
     *
     * @param organizationId organization identifier from the route
     * @param userId user identifier from the route
     * @param resource role assignment request resource
     * @return response entity containing the updated user resource or failure detail
     */
    @Operation(
            summary = "Assign a user role",
            description = "Assigns an existing role to an existing user inside the provided organization",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "User role assignment request",
                    content = @Content(schema = @Schema(implementation = AssignUserRoleResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User role assigned",
                    content = @Content(schema = @Schema(implementation = UserResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization, user or role not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{userId}/role")
    public ResponseEntity<?> assignUserRole(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "userId", description = "User identifier", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody AssignUserRoleResource resource) {
        log.debug("PATCH /organizations/{}/users/{}/role - roleId={}",
                organizationId, userId, resource.roleId());
        var command = AssignUserRoleCommandFromResourceAssembler.toCommandFromResource(
                resource,
                organizationId,
                userId
        );
        var user = userCommandService.handle(command);
        return ResponseEntityFromUserCommandResultAssembler.toResponseEntityFromRoleAssignmentResult(
                user,
                messageSource
        );
    }

    /**
     * Deletes an existing organization user.
     *
     * @param organizationId organization identifier from the route
     * @param userId user identifier from the route
     * @return empty response on success or failure detail
     */
    @Operation(summary = "Delete a user",
            description = "Deletes one user that belongs to the provided organization")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Missing or invalid identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Organization or user not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "User cannot be deleted because related data exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @Parameter(name = "organizationId", description = "Organization identifier", required = true)
            @PathVariable Long organizationId,
            @Parameter(name = "userId", description = "User identifier", required = true)
            @PathVariable Long userId) {
        log.debug("DELETE /organizations/{}/users/{}", organizationId, userId);
        var result = userCommandService.handle(new DeleteUserCommand(organizationId, userId));
        return ResponseEntityFromUserCommandResultAssembler.toResponseEntityFromDeleteResult(
                result,
                messageSource
        );
    }
}
