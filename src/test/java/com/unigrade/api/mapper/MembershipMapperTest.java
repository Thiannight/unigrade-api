package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.unigrade.api.model.Membership;
import com.unigrade.api.repository.model.JGroup;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipMapperTest {
  private final MembershipMapper membershipMapper = new MembershipMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    JGroup group =
        JGroup.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
    JUser student = JUser.builder().id("STD00001").build();
    JMembership entity =
        JMembership.builder()
            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .group(group)
            .student(student)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .build();

    Membership result = membershipMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getGroup().getId(), result.groupId());
    assertEquals(entity.getStudent().getId(), result.studentId());
    assertEquals(entity.getStartDate(), result.startDate());
    assertEquals(entity.getEndDate(), result.endDate());
  }

  @Test
  void toDomain_mapsNullEndDate() {
    JGroup group =
        JGroup.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
    JUser student = JUser.builder().id("STD00001").build();
    JMembership entity =
        JMembership.builder()
            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .group(group)
            .student(student)
            .startDate(LocalDate.of(2024, 1, 1))
            .build();

    Membership result = membershipMapper.toDomain(entity);

    assertNull(result.endDate());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    Membership domain =
        Membership.builder()
            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .groupId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .studentId("STD00001")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .build();
    JGroup group =
        JGroup.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
    JUser student = JUser.builder().id("STD00001").build();

    JMembership result = membershipMapper.toEntity(domain, group, student);

    assertEquals(domain.id(), result.getId());
    assertEquals(group, result.getGroup());
    assertEquals(student, result.getStudent());
    assertEquals(domain.startDate(), result.getStartDate());
    assertEquals(domain.endDate(), result.getEndDate());
  }
}
