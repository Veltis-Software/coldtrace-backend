package com.acme.coldtrace.platform.iam.interfaces.rest;

import com.acme.coldtrace.platform.iam.domain.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Resolves legacy email-only JWTs through the owning IAM context during Strangler migration. */
@RestController
public class SessionContextController {
  private final UserRepository users;
  private final com.acme.coldtrace.platform.iam.domain.repositories.RoleRepository roles;

  public SessionContextController(UserRepository users, com.acme.coldtrace.platform.iam.domain.repositories.RoleRepository roles) {
    this.users = users;
    this.roles = roles;
  }

  @Operation(summary = "Resolve the authenticated organization during service migration")
  @GetMapping("/api/v1/session/context")
  public SessionContext context(Authentication authentication) {
    if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var user =
        users
            .findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    var permissions = roles.findById(user.getRoleId()).map(role -> role.getPermissions().stream()
        .map(permission -> permission.getResource() + ":" + permission.getAction()).toList()).orElse(java.util.List.of());
    return new SessionContext(user.getId(), user.getOrganizationId(),
        user.getFirstName() + " " + user.getLastName(), permissions);
  }

  public record SessionContext(Long userId, Long organizationId, String fullName, java.util.List<String> permissions) {}
}
