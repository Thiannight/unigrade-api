package com.unigrade.api.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Role;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MembershipValidatorTest {

  private final MembershipValidator validator = new MembershipValidator();

  @Test
  void validateStudent_activeStudent_doesNotThrow() {
    assertDoesNotThrow(() -> validator.validateStudent(student(true)));
  }

  @Test
  void validateStudent_notStudent_throwsBadRequest() {
    var teacher = student(true);
    teacher.setRole(Role.TEACHER);

    assertThrows(BadRequestException.class, () -> validator.validateStudent(teacher));
  }

  @Test
  void validateStudent_inactive_throwsBadRequest() {
    assertThrows(BadRequestException.class, () -> validator.validateStudent(student(false)));
  }

  @Test
  void validateStudent_alreadyInGroup_throwsBadRequest() {
    var grouped = student(true);
    grouped.setMemberships(List.of(membership()));

    assertThrows(BadRequestException.class, () -> validator.validateStudent(grouped));
  }

  @Test
  void validateTransferDate_afterStart_doesNotThrow() {
    assertDoesNotThrow(
        () -> validator.validateTransferDate(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)));
  }

  @Test
  void validateTransferDate_beforeStart_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator.validateTransferDate(
                LocalDate.of(2024, 1, 1).minusDays(1), LocalDate.of(2024, 1, 1)));
  }

  private JUser student(boolean active) {
    var student = new JUser();
    student.setId("STD26001");
    student.setRole(Role.STUDENT);
    student.setIsActive(active);
    student.setMemberships(List.of());
    return student;
  }

  private JMembership membership() {
    var group = new JStudentGroup();
    group.setId(java.util.UUID.randomUUID());
    return JMembership.builder()
        .id(java.util.UUID.randomUUID())
        .group(group)
        .student(student(true))
        .startDate(LocalDate.of(2024, 1, 1))
        .endDate(null)
        .build();
  }
}
