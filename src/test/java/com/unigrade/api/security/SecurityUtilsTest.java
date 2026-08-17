package com.unigrade.api.security;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.model.Role;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void currentUser_noAuthentication_throwsForbidden() {
    SecurityContextHolder.clearContext();

    assertThrows(ForbiddenException.class, SecurityUtils::currentUser);
  }

  @Test
  void currentUser_nonPrincipalPrincipal_throwsForbidden() {
    var authentication = new UsernamePasswordAuthenticationToken("anonymous", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThrows(ForbiddenException.class, SecurityUtils::currentUser);
  }

  @Test
  void currentUser_returnsPrincipal() {
    JUser principal = principal();
    var authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertSame(principal, SecurityUtils.currentUser());
  }

  private JUser principal() {
    return JUser.builder()
        .id("STD00001")
        .firstName("Ada")
        .lastName("Lovelace")
        .birthDate(LocalDate.of(2000, 1, 1))
        .email("ada@unigrade.com")
        .password("secret")
        .isActive(true)
        .role(Role.STUDENT)
        .build();
  }
}
