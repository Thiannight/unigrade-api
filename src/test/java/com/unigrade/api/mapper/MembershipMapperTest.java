package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.unigrade.api.model.Membership;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipMapperTest {
  private final MembershipMapper membershipMapper = new MembershipMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var group = new JStudentGroup();
    group.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var student = new JUser();
    student.setId("STD00001");

    var entity = new JMembership();
    entity.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    entity.setGroup(group);
    entity.setStudent(student);
    entity.setStartDate(LocalDate.of(2026, 9, 1));
    entity.setEndDate(null);

    Membership result = membershipMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getGroup().getId(), result.groupId());
    assertEquals(entity.getStudent().getId(), result.studentId());
    assertEquals(entity.getStartDate(), result.startDate());
    assertNull(result.endDate());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new Membership(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "STD00001",
            LocalDate.of(2026, 9, 1),
            null);
    var group = new JStudentGroup();
    group.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var student = new JUser();
    student.setId("STD00001");

    JMembership result = membershipMapper.toEntity(domain, group, student);

    assertEquals(domain.id(), result.getId());
    assertEquals(group, result.getGroup());
    assertEquals(student, result.getStudent());
    assertEquals(domain.startDate(), result.getStartDate());
    assertNull(result.getEndDate());
  }
}
