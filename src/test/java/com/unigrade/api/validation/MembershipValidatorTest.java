package com.unigrade.api.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipValidatorTest {

  private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID NEW_GROUP_ID = UUID.randomUUID();

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
  void validateTransfer_valid_doesNotThrow() {
    assertDoesNotThrow(
        () ->
            validator.validateTransfer(
                student(true), membership(), request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_sameGroup_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator.validateTransfer(
                student(true), membership(), request(GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_dateBeforeStart_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator.validateTransfer(
                student(true), membership(), request(NEW_GROUP_ID, START_DATE.minusDays(1))));
  }

  @Test
  void validateTransfer_notStudent_throwsBadRequest() {
    var teacher = student(true);
    teacher.setRole(Role.TEACHER);

    assertThrows(
        BadRequestException.class,
        () ->
            validator.validateTransfer(
                teacher, membership(), request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_inactive_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator.validateTransfer(
                student(false), membership(), request(NEW_GROUP_ID, transferDate())));
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
    group.setId(GROUP_ID);
    return JMembership.builder()
        .id(UUID.randomUUID())
        .group(group)
        .student(student(true))
        .startDate(START_DATE)
        .endDate(null)
        .build();
  }

  private static GroupTransferRequest request(UUID newGroupId, LocalDate transferDate) {
    return new GroupTransferRequest(newGroupId, transferDate);
  }

  private static LocalDate transferDate() {
    return START_DATE.plusDays(1);
  }
}
