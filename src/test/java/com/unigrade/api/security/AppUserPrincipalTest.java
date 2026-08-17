package com.unigrade.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unigrade.api.model.Role;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppUserPrincipalTest {

  private final AppUserPrincipal principal = principal(true);

  @Test
  void exposesUserIdentity() {
    assertEquals("STD00001", principal.getId());
    assertEquals(Role.STUDENT, principal.getRole());
    assertEquals("STD00001", principal.getUsername());
    assertEquals("secret", principal.getPassword());
  }

  @Test
  void authorities_arePrefixedWithRole() {
    assertEquals(
        List.of("ROLE_STUDENT"),
        principal.getAuthorities().stream().map(Object::toString).toList());
  }

  @Test
  void accountFlags_areAllGranted() {
    assertTrue(principal.isAccountNonExpired());
    assertTrue(principal.isAccountNonLocked());
    assertTrue(principal.isCredentialsNonExpired());
  }

  @Test
  void isEnabled_reflectsUserFlag() {
    assertTrue(principal(true).isEnabled());
    assertFalse(principal(false).isEnabled());
  }

  private AppUserPrincipal principal(boolean active) {
    return new AppUserPrincipal(
        JUser.builder()
            .id("STD00001")
            .firstName("Ada")
            .lastName("Lovelace")
            .birthDate(LocalDate.of(2000, 1, 1))
            .email("ada@unigrade.com")
            .password("secret")
            .isActive(active)
            .role(Role.STUDENT)
            .build());
  }
}
