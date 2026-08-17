package com.unigrade.api.repository.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unigrade.api.model.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JUserTest {

  private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void getCurrentGroup_nullMemberships_returnsNull() {
    var student = new JUser();

    assertNull(student.getCurrentGroup());
  }

  @Test
  void getCurrentGroup_noActiveMembership_returnsNull() {
    var student = new JUser();
    student.setMemberships(List.of(endedMembership()));

    assertNull(student.getCurrentGroup());
  }

  @Test
  void getCurrentGroup_activeMembership_returnsGroup() {
    var student = new JUser();
    student.setMemberships(List.of(activeMembership()));

    assertEquals(GROUP_ID, student.getCurrentGroup().getId());
  }

  @Test
  void username_returnsId() {
    assertEquals("STD00001", user(true).getUsername());
  }

  @Test
  void authorities_arePrefixedWithRole() {
    assertEquals(
        List.of("ROLE_STUDENT"),
        user(true).getAuthorities().stream().map(Object::toString).toList());
  }

  @Test
  void accountFlags_areAllGranted() {
    assertTrue(user(true).isAccountNonExpired());
    assertTrue(user(true).isAccountNonLocked());
    assertTrue(user(true).isCredentialsNonExpired());
  }

  @Test
  void isEnabled_reflectsUserFlag() {
    assertTrue(user(true).isEnabled());
    assertFalse(user(false).isEnabled());
  }

  private JUser user(boolean active) {
    return JUser.builder()
        .id("STD00001")
        .firstName("Ada")
        .lastName("Lovelace")
        .birthDate(LocalDate.of(2000, 1, 1))
        .email("ada@unigrade.com")
        .password("secret")
        .isActive(active)
        .role(Role.STUDENT)
        .build();
  }

  private JMembership endedMembership() {
    return JMembership.builder()
        .id(UUID.randomUUID())
        .group(group())
        .startDate(LocalDate.of(2024, 1, 1))
        .endDate(LocalDate.of(2024, 6, 1))
        .build();
  }

  private JMembership activeMembership() {
    return JMembership.builder()
        .id(UUID.randomUUID())
        .group(group())
        .startDate(LocalDate.of(2024, 1, 1))
        .endDate(null)
        .build();
  }

  private JStudentGroup group() {
    var group = new JStudentGroup();
    group.setId(GROUP_ID);
    return group;
  }
}
