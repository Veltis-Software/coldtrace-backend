package com.acme.coldtrace.platform.iam.interfaces.acl;

import com.acme.coldtrace.platform.iam.domain.repositories.RoleRepository;
import com.acme.coldtrace.platform.iam.domain.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Owning IAM boundary for tenant and permissions; never accepts organization claims from a client.
 */
@Component
public class SessionContextFacade {
  private final UserRepository users;
  private final RoleRepository roles;

  public SessionContextFacade(UserRepository users, RoleRepository roles) {
    this.users = users;
    this.roles = roles;
  }

  public Optional<SessionContext> resolve(String email) {
    if (email == null || email.isBlank()) return Optional.empty();
    return users
        .findByEmail(email)
        .map(
            user -> {
              var permissions =
                  roles
                      .findById(user.getRoleId())
                      .map(
                          role ->
                              role.getPermissions().stream()
                                  .map(
                                      permission ->
                                          permission.getResource() + ":" + permission.getAction())
                                  .toList())
                      .orElse(List.of());
              return new SessionContext(
                  user.getId(),
                  user.getOrganizationId(),
                  user.getFirstName() + " " + user.getLastName(),
                  permissions);
            });
  }

  public record SessionContext(
      Long userId, Long organizationId, String fullName, List<String> permissions) {}
}
