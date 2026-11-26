package com.acme.coldtrace.platform.iam.interfaces.rest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.acme.coldtrace.platform.iam.domain.model.aggregates.User;
import com.acme.coldtrace.platform.iam.domain.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class SessionContextControllerTest {
  @Test
  void resolvesOnlyTheAuthenticatedUsersOrganization() {
    var users = mock(UserRepository.class);
    var user = mock(User.class);
    when(user.getId()).thenReturn(10L);
    when(user.getOrganizationId()).thenReturn(7L);
    when(users.findByEmail("member@example.test")).thenReturn(Optional.of(user));
    var result =
        new SessionContextController(users, mock(com.acme.coldtrace.platform.iam.domain.repositories.RoleRepository.class))
            .context(new UsernamePasswordAuthenticationToken("member@example.test", "unused"));
    assertThat(result.userId()).isEqualTo(10L);
    assertThat(result.organizationId()).isEqualTo(7L);
  }

  @Test
  void rejectsMissingOrUnknownUsers() {
    var users = mock(UserRepository.class);
    var controller = new SessionContextController(users, mock(com.acme.coldtrace.platform.iam.domain.repositories.RoleRepository.class));
    assertThatThrownBy(() -> controller.context(null)).isInstanceOf(ResponseStatusException.class);
    when(users.findByEmail("missing@example.test")).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                controller.context(
                    new UsernamePasswordAuthenticationToken("missing@example.test", "unused")))
        .isInstanceOf(ResponseStatusException.class);
  }
}
