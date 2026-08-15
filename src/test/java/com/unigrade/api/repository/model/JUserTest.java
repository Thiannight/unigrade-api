package com.unigrade.api.repository.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
