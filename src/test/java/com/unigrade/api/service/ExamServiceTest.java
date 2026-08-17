package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.ExamRequest;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.TeacherCourseRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JUser;
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
class ExamServiceTest {

  private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID GROUP_COURSE_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID EXAM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final String TEACHER_ID = "TCR00001";
  private static final String STUDENT_ID = "STD00001";
  private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2024, 6, 1);
  private static final Instant EXAM_DATE = Instant.parse("2024-03-01T09:00:00Z");
  private static final Instant BEFORE_START = Instant.parse("2023-12-01T09:00:00Z");
  private static final Instant AFTER_END = Instant.parse("2024-07-01T09:00:00Z");
  private static final BigDecimal COEFFICIENT = new BigDecimal("0.5000");

  @Mock private ExamRepository repository;
  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private GradeRepository gradeRepository;
  @Mock private TeacherCourseRepository teacherCourseRepository;
  private final ExamMapper mapper = new ExamMapper();
  private ExamService service;

  @BeforeEach
  void setUp() {
    service =
        new ExamService(
            repository, groupCourseRepository, gradeRepository, teacherCourseRepository, mapper);
    loginAs("MGR00001", Role.ADMIN);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(String id, Role role) {
    var principal = userStub(id, role);
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
  void findByGroupAndCourse_returnsMappedList() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam()));

    List<Exam> result = service.findByGroupAndCourse(GROUP_ID, COURSE_ID);

    assertEquals(1, result.size());
    assertEquals(EXAM_ID, result.get(0).id());
    assertEquals(GROUP_COURSE_ID, result.get(0).groupCourseId());
  }

  @Test
  void findByGroupAndCourse_noActiveAssignment_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> service.findByGroupAndCourse(GROUP_ID, COURSE_ID));

    assertTrue(exception.getMessage().contains("No active course assignment"));
  }

  @Test
  void findById_existing_returnsMapped() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));

    Exam result = service.findById(GROUP_ID, COURSE_ID, EXAM_ID);

    assertEquals(EXAM_ID, result.id());
    assertEquals(GROUP_COURSE_ID, result.groupCourseId());
  }

  @Test
  void findById_missingExam_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.findById(GROUP_ID, COURSE_ID, EXAM_ID));

    assertTrue(exception.getMessage().contains("Exam not found"));
  }

  @Test
  void findById_noActiveAssignment_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.findById(GROUP_ID, COURSE_ID, EXAM_ID));
  }

  @Test
  void create_saves() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID)).thenReturn(BigDecimal.ZERO);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Exam result = service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, COEFFICIENT));

    assertEquals(EXAM_DATE, result.examDate());
    assertEquals(COEFFICIENT, result.coefficient());
    assertEquals(GROUP_COURSE_ID, result.groupCourseId());
    verify(repository).save(any());
  }

  @Test
  void create_coefficientNormalizedToStoredScale() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID)).thenReturn(BigDecimal.ZERO);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Exam result =
        service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, new BigDecimal("0.5")));

    assertEquals(new BigDecimal("0.5000"), result.coefficient());
  }

  @Test
  void create_coefficientAtBudget_saves() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(new BigDecimal("0.5000"));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Exam result = service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, COEFFICIENT));

    assertEquals(COEFFICIENT, result.coefficient());
    verify(repository).save(any());
  }

  @Test
  void create_coefficientOverBudget_throwsBadRequest() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(new BigDecimal("0.6000"));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                service.create(
                    GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, new BigDecimal("0.5000"))));

    assertTrue(exception.getMessage().contains("exceed 1"));
  }

  @Test
  void create_noActiveAssignment_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, COEFFICIENT)));
  }

  @Test
  void create_dateBeforeStart_throwsBadRequest() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));

    assertThrows(
        BadRequestException.class,
        () -> service.create(GROUP_ID, COURSE_ID, new ExamRequest(BEFORE_START, COEFFICIENT)));
  }

  @Test
  void create_dateAfterEnd_throwsBadRequest() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));

    assertThrows(
        BadRequestException.class,
        () -> service.create(GROUP_ID, COURSE_ID, new ExamRequest(AFTER_END, COEFFICIENT)));
  }

  @Test
  void create_asStudent_throwsForbidden() {
    loginAs(STUDENT_ID, Role.STUDENT);

    assertThrows(
        ForbiddenException.class,
        () -> service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, COEFFICIENT)));
  }

  @Test
  void create_asTeacherNotAssigned_throwsForbidden() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(false);

    assertThrows(
        ForbiddenException.class,
        () -> service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, COEFFICIENT)));
  }

  @Test
  void create_asTeacherAssigned_isAllowed() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(true);
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID)).thenReturn(BigDecimal.ZERO);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Exam result = service.create(GROUP_ID, COURSE_ID, new ExamRequest(EXAM_DATE, COEFFICIENT));

    assertEquals(GROUP_COURSE_ID, result.groupCourseId());
    verify(repository).save(any());
  }

  @Test
  void update_asTeacherNotAssigned_throwsForbidden() {
    loginAs(TEACHER_ID, Role.TEACHER);
    when(teacherCourseRepository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(false);

    assertThrows(
        ForbiddenException.class,
        () -> service.update(GROUP_ID, COURSE_ID, EXAM_ID, new ExamRequest(EXAM_DATE, COEFFICIENT)));
  }

  @Test
  void delete_asStudent_throwsForbidden() {
    loginAs(STUDENT_ID, Role.STUDENT);

    assertThrows(ForbiddenException.class, () -> service.delete(GROUP_ID, COURSE_ID, EXAM_ID));
  }

  @Test
  void update_updatesExam() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));
    when(repository.sumCoefficientByGroupCourseIdExcluding(GROUP_COURSE_ID, EXAM_ID))
        .thenReturn(BigDecimal.ZERO);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Exam result =
        service.update(GROUP_ID, COURSE_ID, EXAM_ID, new ExamRequest(EXAM_DATE, COEFFICIENT));

    assertEquals(COEFFICIENT, result.coefficient());
    assertEquals(EXAM_ID, result.id());
    verify(repository).save(any());
  }

  @Test
  void update_coefficientWithinBudget_updates() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));
    when(repository.sumCoefficientByGroupCourseIdExcluding(GROUP_COURSE_ID, EXAM_ID))
        .thenReturn(new BigDecimal("0.6000"));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Exam result =
        service.update(
            GROUP_ID, COURSE_ID, EXAM_ID, new ExamRequest(EXAM_DATE, new BigDecimal("0.4000")));

    assertEquals(new BigDecimal("0.4000"), result.coefficient());
    verify(repository).save(any());
  }

  @Test
  void update_coefficientOverBudget_throwsBadRequest() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));
    when(repository.sumCoefficientByGroupCourseIdExcluding(GROUP_COURSE_ID, EXAM_ID))
        .thenReturn(new BigDecimal("0.6000"));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                service.update(
                    GROUP_ID,
                    COURSE_ID,
                    EXAM_ID,
                    new ExamRequest(EXAM_DATE, new BigDecimal("0.7000"))));

    assertTrue(exception.getMessage().contains("exceed 1"));
  }

  @Test
  void update_missingExam_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                service.update(
                    GROUP_ID, COURSE_ID, EXAM_ID, new ExamRequest(EXAM_DATE, COEFFICIENT)));

    assertTrue(exception.getMessage().contains("Exam not found"));
  }

  @Test
  void update_dateBeforeStart_throwsBadRequest() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));

    assertThrows(
        BadRequestException.class,
        () ->
            service.update(
                GROUP_ID, COURSE_ID, EXAM_ID, new ExamRequest(BEFORE_START, COEFFICIENT)));
  }

  @Test
  void update_noActiveAssignment_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () ->
            service.update(GROUP_ID, COURSE_ID, EXAM_ID, new ExamRequest(EXAM_DATE, COEFFICIENT)));
  }

  @Test
  void delete_deletes() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));
    when(gradeRepository.existsByExamId(EXAM_ID)).thenReturn(false);

    service.delete(GROUP_ID, COURSE_ID, EXAM_ID);

    verify(repository).delete(any(JExam.class));
  }

  @Test
  void delete_gradesExist_throwsConflict() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(exam()));
    when(gradeRepository.existsByExamId(EXAM_ID)).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.delete(GROUP_ID, COURSE_ID, EXAM_ID));
  }

  @Test
  void delete_missingExam_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(assignment()));
    when(repository.findByIdAndGroupCourseId(EXAM_ID, GROUP_COURSE_ID))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.delete(GROUP_ID, COURSE_ID, EXAM_ID));
  }

  @Test
  void delete_noActiveAssignment_throwsNotFound() {
    when(groupCourseRepository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.delete(GROUP_ID, COURSE_ID, EXAM_ID));
  }

  private JGroupCourse assignment() {
    return JGroupCourse.builder()
        .id(GROUP_COURSE_ID)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .build();
  }

  private JExam exam() {
    var groupCourse = new JGroupCourse();
    groupCourse.setId(GROUP_COURSE_ID);
    return JExam.builder()
        .id(EXAM_ID)
        .examDate(EXAM_DATE)
        .coefficient(COEFFICIENT)
        .groupCourse(groupCourse)
        .build();
  }
}
