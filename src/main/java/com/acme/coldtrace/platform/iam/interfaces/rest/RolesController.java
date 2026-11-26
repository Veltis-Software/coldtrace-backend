package com.acme.coldtrace.platform.iam.interfaces.rest;

import com.acme.coldtrace.platform.iam.application.queryservices.RoleQueryService;
import com.acme.coldtrace.platform.iam.domain.model.queries.GetAllRolesQuery;
import com.acme.coldtrace.platform.iam.interfaces.rest.resources.RoleResource;
import com.acme.coldtrace.platform.iam.interfaces.rest.transform.ResponseEntityFromRoleQueryResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller exposing role endpoints.
 * It provides role and permission metadata required by the frontend.
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/roles", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Roles", description = "Endpoints for roles and permissions")
public class RolesController {
    private final RoleQueryService roleQueryService;

    public RolesController(RoleQueryService roleQueryService) {
        this.roleQueryService = roleQueryService;
    }

    /**
     * Gets all roles and their permissions.
     *
     * @return response entity containing role resources
     */
    @Operation(summary = "Get all roles", description = "Gets all roles and their permissions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles found",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleResource.class))))
    })
    @GetMapping
    public ResponseEntity<?> getAllRoles() {
        log.debug("GET /roles");
        var roles = roleQueryService.handle(new GetAllRolesQuery());
        return ResponseEntityFromRoleQueryResultAssembler.toResponseEntityFromList(roles);
    }
}
