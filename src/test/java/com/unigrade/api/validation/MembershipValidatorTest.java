package com.unigrade.api.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.GradeCalculationService;
import com.unigrade.api.service.GradeCalculationService.CourseResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipValidatorTest {

  private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID NEW_GROUP_ID = UUID.randomUUID();

  @Mock private GradeCalculationService gradeCalculationService;
  @Mock private GroupCourseRepository groupCourseRepository;

  private MembershipValidator validator() {
    return new MembershipValidator(gradeCalculationService, groupCourseRepository);
  }

  @Test
  void validateTransfer_valid_doesNotThrow() {
    mockCompleteCourses();
    assertDoesNotThrow(
        () ->
            validator()
                .validateTransfer(
                    student(true), membership(), request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_sameGroup_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator()
                .validateTransfer(student(true), membership(), request(GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_dateBeforeStart_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator()
                .validateTransfer(
                    student(true), membership(), request(NEW_GROUP_ID, START_DATE.minusDays(1))));
  }

  @Test
  void validateTransfer_notStudent_throwsBadRequest() {
    var teacher = student(true);
    teacher.setRole(Role.TEACHER);

    assertThrows(
        BadRequestException.class,
        () ->
            validator()
                .validateTransfer(teacher, membership(), request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_inactive_throwsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            validator()
                .validateTransfer(
                    student(false), membership(), request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_nullOldMembership_doesNotThrow() {
    assertDoesNotThrow(
        () ->
            validator()
                .validateTransfer(student(true), null, request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_insufficientCredits_throwsBadRequest() {
    when(groupCourseRepository.findAllByGroupId(GROUP_ID))
        .thenReturn(List.of(groupCourse((short) 6)));

    assertThrows(
        BadRequestException.class,
        () ->
            validator()
                .validateTransfer(
                    student(true), membership(), request(NEW_GROUP_ID, transferDate())));
  }

  @Test
  void validateTransfer_incompleteCourses_throwsBadRequest() {
    mockIncompleteCourses();

    assertThrows(
        BadRequestException.class,
        () ->
            validator()
                .validateTransfer(
                    student(true), membership(), request(NEW_GROUP_ID, transferDate())));
  }

  private void mockCompleteCourses() {
    when(groupCourseRepository.findAllByGroupId(GROUP_ID))
        .thenReturn(List.of(groupCourse((short) 15), groupCourse((short) 15)));
    when(gradeCalculationService.computeCourseResult(any(), anyString(), anyList()))
        .thenReturn(new CourseResult(true, BigDecimal.TEN, List.of()));
  }

  private void mockIncompleteCourses() {
    when(groupCourseRepository.findAllByGroupId(GROUP_ID))
        .thenReturn(List.of(groupCourse((short) 15), groupCourse((short) 15)));
    when(gradeCalculationService.computeCourseResult(any(), anyString(), anyList()))
        .thenReturn(new CourseResult(false, BigDecimal.TEN, List.of()));
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

  private JGroupCourse groupCourse(short credits) {
    var course = new JCourse();
    course.setCredits(credits);
    return JGroupCourse.builder()
        .id(UUID.randomUUID())
        .course(course)
        .group(new JStudentGroup())
        .build();
  }

  private static GroupTransferRequest request(UUID newGroupId, LocalDate transferDate) {
    return new GroupTransferRequest(newGroupId, transferDate);
  }

  private static LocalDate transferDate() {
    return START_DATE.plusDays(1);
  }
}
