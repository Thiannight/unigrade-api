package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.ExamScore;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.Semester;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.GradeCalculationService.CourseKey;
import com.unigrade.api.service.GradeCalculationService.CourseParticipation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
class ReportServiceTest {

  private static final String STUDENT_ID = "STD24001";
  private static final UUID GROUP_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID GROUP_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID PROMOTION_ID_1 =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID PROMOTION_ID_2 =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID COURSE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID GROUP_COURSE_ID_1 =
      UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID GROUP_COURSE_ID_2 =
      UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID EXAM_ID_1 = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID EXAM_ID_2 = UUID.fromString("99999999-9999-9999-9999-999999999999");

  @Mock private UserRepository userRepository;
  @Mock private GradeCalculationService gradeCalculationService;

  private ReportService service;

  @BeforeEach
  void setUp() {
    service = new ReportService(userRepository, gradeCalculationService);
    loginAs("ADMIN001", Role.ADMIN);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void loginAs(String id, Role role) {
    var principal = new JUser();
    principal.setId(id);
    principal.setRole(role);
    principal.setIsActive(true);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  @Test
  void generate_happyPath_buildsLevelReport() {
    JUser student = student();
    JPromotion promo = promotion("P-2024", (short) 2024);
    JStudentGroup group = group(GROUP_ID_1, promo);
    JGroupCourse groupCourse = groupCourse(GROUP_COURSE_ID_1, group, Semester.S3);

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    stubResolvedCourses(
        Level.L2,
        new CourseKey(PROMOTION_ID_1, COURSE_ID),
        new CourseParticipation(group, promo, groupCourse));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_1, STUDENT_ID))
        .thenReturn(
            List.of(
                new ExamScore(
                    EXAM_ID_1,
                    Instant.parse("2024-05-01T09:00:00Z"),
                    new BigDecimal("0.4"),
                    new BigDecimal("10.0")),
                new ExamScore(
                    EXAM_ID_2,
                    Instant.parse("2024-06-01T09:00:00Z"),
                    new BigDecimal("0.6"),
                    new BigDecimal("16.0"))));
    when(gradeCalculationService.isCourseComplete(GROUP_COURSE_ID_1, STUDENT_ID)).thenReturn(true);
    when(gradeCalculationService.averageFromExamScores(any())).thenReturn(new BigDecimal("13.60"));

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(STUDENT_ID, report.studentId());
    assertEquals("Alice", report.firstName());
    assertEquals("Dupont", report.lastName());
    assertEquals(1, report.levels().size());
    assertEquals(Level.L2, report.levels().getFirst().level());
    assertEquals(1, report.levels().getFirst().courses().size());
    var course = report.levels().getFirst().courses().getFirst();
    assertEquals(COURSE_ID, course.courseId());
    assertEquals("P-2024", course.promotionReference());
    assertEquals((short) 6, course.credits());
    assertTrue(course.completed());
    assertEquals(2, course.exams().size());
    assertEquals(new BigDecimal("10.0"), course.exams().getFirst().score());
    assertEquals(new BigDecimal("16.0"), course.exams().get(1).score());
    assertEquals(new BigDecimal("13.60"), course.average());
    assertEquals(new BigDecimal("13.60"), report.levels().getFirst().overallAverage());
    assertEquals(ReportStatus.TEMPORARY, report.levels().getFirst().status());
    assertEquals(6, report.levels().getFirst().totalCredits());
    assertEquals(60, report.levels().getFirst().requiredCredits());
    assertEquals(new BigDecimal("13.60"), report.overallAverage());
    assertEquals(ReportStatus.TEMPORARY, report.status());
    assertEquals(6, report.totalCredits());
    assertEquals(180, report.requiredCredits());
  }

  @Test
  void generate_missingStudent_throwsNotFound() {
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.generate(STUDENT_ID, null));
  }

  @Test
  void generate_nonStudent_throwsBadRequest() {
    JUser teacher = JUser.builder().id(STUDENT_ID).role(Role.TEACHER).build();
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(teacher));

    assertThrows(BadRequestException.class, () -> service.generate(STUDENT_ID, null));
  }

  @Test
  void generate_noMemberships_returnsEmptyReport() {
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(gradeCalculationService.resolveCoursesByLevel(eq(STUDENT_ID), any(Level.class)))
        .thenReturn(Map.of());

    StudentReport report = service.generate(STUDENT_ID, null);

    assertTrue(report.levels().isEmpty());
    assertEquals(ReportStatus.TEMPORARY, report.status());
    assertEquals(180, report.requiredCredits());
    assertNull(report.overallAverage());
  }

  @Test
  void generate_onlyL1Data_reportShows180RequiredAndTemporary() {
    JUser student = student();
    JPromotion promo = promotion("P-2024", (short) 2024);
    JStudentGroup g = group(GROUP_ID_1, promo);
    JGroupCourse l1Course = groupCourse(GROUP_COURSE_ID_1, g, Semester.S1);

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    stubResolvedCourses(
        Level.L1,
        new CourseKey(PROMOTION_ID_1, COURSE_ID),
        new CourseParticipation(g, promo, l1Course));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_1, STUDENT_ID))
        .thenReturn(List.of());

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(1, report.levels().size());
    assertEquals(Level.L1, report.levels().getFirst().level());
    assertEquals(180, report.requiredCredits());
    assertEquals(ReportStatus.TEMPORARY, report.status());
  }

  @Test
  void generate_repeat_keepsOnlyLatestPromotionPerLevel() {
    JUser student = student();
    JPromotion oldPromo = promotion("P-2022", (short) 2022);
    JPromotion newPromo = promotion("P-2024", (short) 2024);
    JStudentGroup group1 = group(GROUP_ID_1, oldPromo);
    JStudentGroup group2 = group(GROUP_ID_2, newPromo);
    JGroupCourse oldCourse = groupCourse(GROUP_COURSE_ID_1, group1, Semester.S3);
    JGroupCourse newCourse = groupCourse(GROUP_COURSE_ID_2, group2, Semester.S3);

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    stubResolvedCourses(
        Level.L2,
        new CourseKey(PROMOTION_ID_2, COURSE_ID),
        new CourseParticipation(group2, newPromo, newCourse));

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(1, report.levels().size());
    assertEquals(1, report.levels().getFirst().courses().size());
    assertEquals("P-2024", report.levels().getFirst().courses().getFirst().promotionReference());
  }

  @Test
  void generate_excludesExamWhenNotMemberAtExamDate() {
    JUser student = student();
    JPromotion promo = promotion("P-2024", (short) 2024);
    JStudentGroup g = group(GROUP_ID_1, promo);
    JGroupCourse groupCourse = groupCourse(GROUP_COURSE_ID_1, g, Semester.S1);

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    stubResolvedCourses(
        Level.L1,
        new CourseKey(PROMOTION_ID_1, COURSE_ID),
        new CourseParticipation(g, promo, groupCourse));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_1, STUDENT_ID))
        .thenReturn(List.of());

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(Level.L1, report.levels().getFirst().level());
    var course = report.levels().getFirst().courses().getFirst();
    assertTrue(course.exams().isEmpty());
    assertNull(course.average());
    assertFalse(course.completed());
    assertNull(report.levels().getFirst().overallAverage());
  }

  @Test
  void generate_transferWithinPromotion_usesFirstGroupExamsOnly() {
    JUser student = student();
    JPromotion promo = promotion("P-2024", (short) 2024);
    JStudentGroup group1 = group(GROUP_ID_1, promo);
    JStudentGroup group2 = group(GROUP_ID_2, promo);
    JGroupCourse firstAssignment = groupCourse(GROUP_COURSE_ID_1, group1, Semester.S4);
    JGroupCourse secondAssignment = groupCourse(GROUP_COURSE_ID_2, group2, Semester.S4);

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    stubResolvedCourses(
        Level.L2,
        new CourseKey(PROMOTION_ID_1, COURSE_ID),
        new CourseParticipation(group1, promo, firstAssignment),
        new CourseKey(PROMOTION_ID_1, UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA")),
        new CourseParticipation(group2, promo, secondAssignment));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_1, STUDENT_ID))
        .thenReturn(
            List.of(
                new ExamScore(
                    EXAM_ID_1,
                    Instant.parse("2024-05-01T09:00:00Z"),
                    new BigDecimal("0.5"),
                    new BigDecimal("10.0"))));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_2, STUDENT_ID))
        .thenReturn(List.of());

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(1, report.levels().size());
    assertEquals(2, report.levels().getFirst().courses().size());
    var course = report.levels().getFirst().courses().getFirst();
    assertEquals(1, course.exams().size());
    assertEquals(new BigDecimal("10.0"), course.exams().getFirst().score());
  }

  @Test
  void generate_transferWithinPromotion_newGroupCourseAppears() {
    JUser student = student();
    UUID courseIdOld = UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA");
    UUID courseIdNew = UUID.fromString("BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB");
    JPromotion promo = promotion("P-2024", (short) 2024);
    JStudentGroup g1 = group(GROUP_ID_1, promo);
    JStudentGroup g2 = group(GROUP_ID_2, promo);
    JGroupCourse g1Course =
        JGroupCourse.builder()
            .id(GROUP_COURSE_ID_1)
            .group(g1)
            .course(
                JCourse.builder()
                    .id(courseIdOld)
                    .reference("C-OLD")
                    .title("Old")
                    .credits((short) 6)
                    .build())
            .semester(Semester.S4)
            .startDate(LocalDate.of(2024, 1, 1))
            .build();
    JGroupCourse g2Course =
        JGroupCourse.builder()
            .id(GROUP_COURSE_ID_2)
            .group(g2)
            .course(
                JCourse.builder()
                    .id(courseIdNew)
                    .reference("C-NEW")
                    .title("New")
                    .credits((short) 6)
                    .build())
            .semester(Semester.S4)
            .startDate(LocalDate.of(2024, 1, 1))
            .build();

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    stubResolvedCourses(
        Level.L2,
        new CourseKey(PROMOTION_ID_1, courseIdOld),
        new CourseParticipation(g1, promo, g1Course),
        new CourseKey(PROMOTION_ID_1, courseIdNew),
        new CourseParticipation(g2, promo, g2Course));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_1, STUDENT_ID))
        .thenReturn(List.of());
    when(gradeCalculationService.isCourseComplete(GROUP_COURSE_ID_1, STUDENT_ID)).thenReturn(false);
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_2, STUDENT_ID))
        .thenReturn(
            List.of(
                new ExamScore(
                    UUID.fromString("CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC"),
                    Instant.parse("2024-07-01T09:00:00Z"),
                    new BigDecimal("1.0"),
                    new BigDecimal("14.0"))));
    when(gradeCalculationService.isCourseComplete(GROUP_COURSE_ID_2, STUDENT_ID)).thenReturn(true);
    when(gradeCalculationService.averageFromExamScores(any()))
        .thenAnswer(
            inv -> {
              List<ExamScore> scores = inv.getArgument(0);
              return scores.isEmpty() ? null : new BigDecimal("14.00");
            });

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(1, report.levels().size());
    var courses = report.levels().getFirst().courses();
    assertEquals(2, courses.size());
    assertEquals(courseIdOld, courses.get(0).courseId());
    assertEquals(courseIdNew, courses.get(1).courseId());
    assertEquals(new BigDecimal("14.00"), courses.get(1).average());
  }

  @Test
  void generate_withLevelFilter_returnsOnlyThatLevel() {
    JUser student = student();
    JPromotion promo = promotion("P-2024", (short) 2024);
    JStudentGroup g1 = group(GROUP_ID_1, promo);
    JStudentGroup g2 = group(GROUP_ID_2, promo);
    JGroupCourse l1Course = groupCourse(GROUP_COURSE_ID_1, g1, Semester.S1);
    JGroupCourse l2Course = groupCourse(GROUP_COURSE_ID_2, g2, Semester.S3);

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(gradeCalculationService.resolveCoursesByLevel(STUDENT_ID, Level.L2))
        .thenReturn(
            Map.of(
                new CourseKey(PROMOTION_ID_1, COURSE_ID),
                new CourseParticipation(g2, promo, l2Course)));
    when(gradeCalculationService.collectExamScores(GROUP_COURSE_ID_2, STUDENT_ID))
        .thenReturn(List.of());

    StudentReport report = service.generate(STUDENT_ID, Level.L2);

    assertEquals(1, report.levels().size());
    assertEquals(Level.L2, report.levels().getFirst().level());
  }

  @Test
  void generate_studentViewingOwnReport_isAllowed() {
    loginAs(STUDENT_ID, Role.STUDENT);
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(gradeCalculationService.resolveCoursesByLevel(eq(STUDENT_ID), any(Level.class)))
        .thenReturn(Map.of());

    StudentReport report = service.generate(STUDENT_ID, null);

    assertEquals(STUDENT_ID, report.studentId());
  }

  @Test
  void generate_studentViewingOtherStudent_throwsForbidden() {
    loginAs("OTHER001", Role.STUDENT);

    assertThrows(ForbiddenException.class, () -> service.generate(STUDENT_ID, null));
  }

  @Test
  void generate_teacherViewingReport_throwsForbidden() {
    loginAs("TEACHER001", Role.TEACHER);

    assertThrows(ForbiddenException.class, () -> service.generate(STUDENT_ID, null));
  }

  private void stubResolvedCourses(
      Level level, CourseKey key, CourseParticipation... participations) {
    Map<CourseKey, CourseParticipation> deduped = new LinkedHashMap<>();
    deduped.put(key, participations[0]);
    stubAllLevels(level, deduped);
  }

  private void stubResolvedCourses(
      Level level, CourseKey key1, CourseParticipation p1, CourseKey key2, CourseParticipation p2) {
    Map<CourseKey, CourseParticipation> deduped = new LinkedHashMap<>();
    deduped.put(key1, p1);
    deduped.put(key2, p2);
    stubAllLevels(level, deduped);
  }

  private void stubAllLevels(Level targetLevel, Map<CourseKey, CourseParticipation> targetCourses) {
    for (Level level : Level.values()) {
      when(gradeCalculationService.resolveCoursesByLevel(STUDENT_ID, level))
          .thenReturn(level == targetLevel ? targetCourses : Map.of());
    }
  }

  private JUser student() {
    return JUser.builder()
        .id(STUDENT_ID)
        .role(Role.STUDENT)
        .firstName("Alice")
        .lastName("Dupont")
        .build();
  }

  private JPromotion promotion(String reference, Short startYear) {
    return JPromotion.builder()
        .id(UUID.randomUUID())
        .reference(reference)
        .startYear(startYear)
        .endYear((short) (startYear + 1))
        .build();
  }

  private JStudentGroup group(UUID id, JPromotion promotion) {
    return JStudentGroup.builder().id(id).reference("A1").promotion(promotion).build();
  }

  private JGroupCourse groupCourse(UUID groupCourseId, JStudentGroup group, Semester semester) {
    return JGroupCourse.builder()
        .id(groupCourseId)
        .group(group)
        .course(
            JCourse.builder()
                .id(COURSE_ID)
                .reference("C-REF")
                .title("Course")
                .credits((short) 6)
                .build())
        .semester(semester)
        .startDate(LocalDate.of(2024, 1, 1))
        .build();
  }
}
