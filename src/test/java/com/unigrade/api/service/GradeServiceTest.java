package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.GradeMapper;
import com.unigrade.api.model.Grade;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.GradeRequest;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.TeacherCourseRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.AppUserPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

  private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID GROUP_COURSE_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID EXAM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID GRADE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final String STUDENT_ID = "STD00001";
  private static final String OTHER_STUDENT_ID = "STD00002";
  private static final String TEACHER_ID = "TCR00001";
  private static final String ADMIN_ID = "MGR00001";
  private static final Instant EXAM_DATE = Instant.parse("2024-03-01T09:00:00Z");
  private static final Instant GRADE_DATE = Instant.parse("2024-03-02T09:00:00Z");

  @Mock private GradeRepository repository;
  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private ExamRepository examRepository;
  @Mock private UserRepository userRepository;
  @Mock private MembershipRepository membershipRepository;
  @Mock private TeacherCourseRepository teacherCourseRepository;
  private final GradeMapper mapper = new GradeMapper();
  private GradeService service;

  @BeforeEach
  void setUp() {
    service =
        new GradeService(
            repository,
            groupCourseRepository,
            examRepository,
            userRepository,
            membershipRepository,
            teacherCourseRepository,
            mapper);
    loginAs(ADMIN_ID, Role.ADMIN);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(String id, Role role) {
    var principal = new AppUserPrincipal(userStub(id, role));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  private JUser userStub(String id, Role role) {
    var user = new JUser();
    user.setId(id);
    user.setRole(role);
    user.setIsActive(true);
    return user;
  }

  @Test
  void findByExam_returnsMappedList() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(repository.findByExamIdOrderByGradeDateAsc(EXAM_ID)).thenReturn(List.of(grade()));

    List<Grade> result = service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, null);

    assertEquals(1, result.size());
    assertEquals(GRADE_ID, result.get(0).id());
    assertEquals(STUDENT_ID, result.get(0).studentId());
  }

  @Test
  void findByExam_filtersByStudentId() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(repository.findByExamIdOrderByGradeDateAsc(EXAM_ID))
        .thenReturn(List.of(grade(), gradeOf(OTHER_STUDENT_ID)));

    List<Grade> result = service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, STUDENT_ID);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.get(0).studentId());
  }

  @Test
  void findByExam_noActiveAssignment_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class, () -> service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, null));
  }

  @Test
  void findByExam_missingExam_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, null));

    assertTrue(exception.getMessage().contains("Exam not found"));
  }

  @Test
  void grade_saves() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Grade result = service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request());

    assertEquals(STUDENT_ID, result.studentId());
    assertEquals(EXAM_ID, result.examId());
    assertEquals(15.5f, result.score());
    assertEquals(GRADE_DATE, result.gradeDate());
    verify(repository).save(any());
  }

  @Test
  void grade_missingStudent_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));

    assertTrue(exception.getMessage().contains("Student not found"));
  }

  @Test
  void grade_notStudentRole_throwsBadRequest() {
    var teacher = student();
    teacher.setRole(Role.TEACHER);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(teacher));

    assertThrows(
        BadRequestException.class, () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));
  }

  @Test
  void grade_inactiveStudent_throwsBadRequest() {
    var inactive = student();
    inactive.setIsActive(false);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(inactive));

    assertThrows(
        BadRequestException.class, () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));
  }

  @Test
  void grade_notMemberAtExamDate_throwsBadRequest() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(false);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));

    assertTrue(exception.getMessage().contains("not a member"));
  }

  @Test
  void grade_missingExam_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class, () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));
  }

  @Test
  void findByExam_studentRequestingOwnGrades_isAllowed() {
    loginAs(STUDENT_ID, Role.STUDENT);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(repository.findByExamIdOrderByGradeDateAsc(EXAM_ID))
        .thenReturn(List.of(grade(), gradeOf(OTHER_STUDENT_ID)));

    List<Grade> result = service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, null);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.get(0).studentId());
  }

  @Test
  void findByExam_studentRequestingSomeoneElsesGrades_throwsForbidden() {
    loginAs(STUDENT_ID, Role.STUDENT);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));

    assertThrows(
        ForbiddenException.class,
        () -> service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, OTHER_STUDENT_ID));
  }

  @Test
  void findByExam_teacherAssignedToCourse_isAllowed() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(true);
    when(repository.findByExamIdOrderByGradeDateAsc(EXAM_ID)).thenReturn(List.of(grade()));

    List<Grade> result = service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, null);

    assertEquals(1, result.size());
  }

  @Test
  void findByExam_teacherNotAssignedToCourse_throwsForbidden() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(false);

    assertThrows(
        ForbiddenException.class, () -> service.findByExam(GROUP_ID, COURSE_ID, EXAM_ID, null));
  }

  @Test
  void grade_asStudent_throwsForbidden() {
    loginAs(STUDENT_ID, Role.STUDENT);

    assertThrows(
        ForbiddenException.class, () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));
  }

  @Test
  void grade_asTeacherNotAssignedToCourse_throwsForbidden() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(false);

    assertThrows(
        ForbiddenException.class, () -> service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request()));
  }

  @Test
  void grade_asTeacherAssignedToCourse_isAllowed() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(true);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(examRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Grade result = service.grade(GROUP_ID, COURSE_ID, EXAM_ID, request());

    assertEquals(STUDENT_ID, result.studentId());
  }

  private JGroupCourse assignment() {
    var group = new JStudentGroup();
    group.setId(GROUP_ID);
    return JGroupCourse.builder()
        .id(GROUP_COURSE_ID)
        .group(group)
        .startDate(LocalDate.of(2024, 1, 1))
        .endDate(null)
        .build();
  }

  private JExam exam() {
    var groupCourse = new JGroupCourse();
    groupCourse.setId(GROUP_COURSE_ID);
    return JExam.builder()
        .id(EXAM_ID)
        .examDate(EXAM_DATE)
        .coefficient(new BigDecimal("0.5000"))
        .groupCourse(groupCourse)
        .build();
  }

  private JUser student() {
    var student = new JUser();
    student.setId(STUDENT_ID);
    student.setRole(Role.STUDENT);
    student.setIsActive(true);
    return student;
  }

  private JGrade grade() {
    return gradeOf(STUDENT_ID);
  }

  private JGrade gradeOf(String studentId) {
    var student = new JUser();
    student.setId(studentId);
    return JGrade.builder()
        .id(GRADE_ID)
        .score(15.5f)
        .gradeDate(GRADE_DATE)
        .reason("Midterm")
        .student(student)
        .exam(exam())
        .build();
  }

  private GradeRequest request() {
    return new GradeRequest(STUDENT_ID, 15.5f, GRADE_DATE, "Midterm");
  }
}
