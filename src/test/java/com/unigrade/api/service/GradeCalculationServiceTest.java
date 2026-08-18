package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.unigrade.api.model.Level;
import com.unigrade.api.model.Semester;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.service.GradeCalculationService.CourseData;
import com.unigrade.api.service.GradeCalculationService.CourseResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeCalculationServiceTest {

  private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID GROUP_COURSE_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID COURSE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID EXAM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final String STUDENT_ID = "STD00001";

  @Mock private MembershipRepository membershipRepository;
  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private ExamRepository examRepository;
  @Mock private GradeRepository gradeRepository;

  private GradeCalculationService service;

  @BeforeEach
  void setUp() {
    service =
        new GradeCalculationService(
            membershipRepository, groupCourseRepository, examRepository, gradeRepository);
  }

  @Test
  void computeCourseResult_withGrades_returnsAverageAndScores() {
    JExam exam = exam("1.0", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(gradeRepository.findByStudentIdAndExamIdsOrderByGradeDateDesc(eq(STUDENT_ID), any()))
        .thenReturn(List.of(grade(exam, 14.0f)));
    List<JMembership> memberships = List.of(membership(LocalDate.of(2024, 1, 1), null));

    CourseResult result = service.computeCourseResult(GROUP_COURSE_ID, STUDENT_ID, memberships);

    assertTrue(result.completed());
    assertEquals(0, new BigDecimal("14.0").compareTo(result.average()));
    assertEquals(1, result.exams().size());
    assertEquals(new BigDecimal("14.0"), result.exams().getFirst().score());
  }

  @Test
  void computeCourseResult_noExams_returnsNullAverage() {
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of());

    CourseResult result =
        service.computeCourseResult(
            GROUP_COURSE_ID, STUDENT_ID, List.of(membership(LocalDate.of(2024, 1, 1), null)));

    assertFalse(result.completed());
    assertNull(result.average());
    assertTrue(result.exams().isEmpty());
  }

  @Test
  void computeCourseResult_notMemberAtExamDate_skipsExam() {
    JExam exam = exam("1.0", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    JMembership expiredMembership =
        JMembership.builder()
            .group(JStudentGroup.builder().id(GROUP_ID).build())
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 3, 1))
            .build();

    CourseResult result =
        service.computeCourseResult(GROUP_COURSE_ID, STUDENT_ID, List.of(expiredMembership));

    assertTrue(result.exams().isEmpty());
    assertFalse(result.completed());
  }

  @Test
  void computeCourseResult_noGrade_defaultsToZero() {
    JExam exam = exam("1.0", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(gradeRepository.findByStudentIdAndExamIdsOrderByGradeDateDesc(eq(STUDENT_ID), any()))
        .thenReturn(List.of());

    CourseResult result =
        service.computeCourseResult(
            GROUP_COURSE_ID, STUDENT_ID, List.of(membership(LocalDate.of(2024, 1, 1), null)));

    assertEquals(0, BigDecimal.ZERO.compareTo(result.average()));
    assertTrue(result.completed());
  }

  @Test
  void computeCourseResult_partialCoefficients_notCompleted() {
    JExam exam1 =
        JExam.builder()
            .id(UUID.randomUUID())
            .examDate(Instant.parse("2024-02-01T09:00:00Z"))
            .coefficient(new BigDecimal("0.6"))
            .groupCourse(
                JGroupCourse.builder()
                    .id(GROUP_COURSE_ID)
                    .group(JStudentGroup.builder().id(GROUP_ID).build())
                    .build())
            .build();
    JExam exam2 =
        JExam.builder()
            .id(UUID.randomUUID())
            .examDate(Instant.parse("2024-06-01T09:00:00Z"))
            .coefficient(new BigDecimal("0.4"))
            .groupCourse(
                JGroupCourse.builder()
                    .id(GROUP_COURSE_ID)
                    .group(JStudentGroup.builder().id(GROUP_ID).build())
                    .build())
            .build();
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam1, exam2));
    when(gradeRepository.findByStudentIdAndExamIdsOrderByGradeDateDesc(eq(STUDENT_ID), any()))
        .thenReturn(List.of(grade(exam1, 12.0f)));
    JMembership membership =
        JMembership.builder()
            .group(JStudentGroup.builder().id(GROUP_ID).build())
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 4, 1))
            .build();

    CourseResult result =
        service.computeCourseResult(GROUP_COURSE_ID, STUDENT_ID, List.of(membership));

    assertFalse(result.completed());
    assertEquals(1, result.exams().size());
  }

  @Test
  void resolveAllCoursesByLevels_noMemberships_returnsEmpty() {
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID)).thenReturn(List.of());

    CourseData result = service.resolveAllCoursesByLevels(STUDENT_ID);

    assertTrue(result.coursesByLevel().isEmpty());
    assertTrue(result.memberships().isEmpty());
  }

  @Test
  void resolveAllCoursesByLevels_withCourses_returnsMap() {
    JPromotion promo = promotion();
    JStudentGroup group = JStudentGroup.builder().id(GROUP_ID).promotion(promo).build();
    JGroupCourse gc =
        JGroupCourse.builder()
            .id(GROUP_COURSE_ID)
            .group(group)
            .course(
                JCourse.builder()
                    .id(COURSE_ID)
                    .reference("C-REF")
                    .title("Course")
                    .credits((short) 6)
                    .build())
            .semester(Semester.S1)
            .startDate(LocalDate.of(2024, 1, 1))
            .build();
    JMembership m = membership(LocalDate.of(2024, 1, 1), null);

    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(List.of(m));
    when(groupCourseRepository.findAllByGroupIdIn(any())).thenReturn(List.of(gc));

    CourseData result = service.resolveAllCoursesByLevels(STUDENT_ID);

    assertEquals(1, result.coursesByLevel().size());
    assertTrue(result.coursesByLevel().containsKey(Level.L1));
    assertEquals(1, result.memberships().size());
  }

  @Test
  void resolveAllCoursesByLevels_groupCourseOutsideMembership_skips() {
    JPromotion promo = promotion();
    JStudentGroup group = JStudentGroup.builder().id(GROUP_ID).promotion(promo).build();
    JGroupCourse gc =
        JGroupCourse.builder()
            .id(GROUP_COURSE_ID)
            .group(group)
            .course(
                JCourse.builder()
                    .id(COURSE_ID)
                    .reference("C-REF")
                    .title("Course")
                    .credits((short) 6)
                    .build())
            .semester(Semester.S1)
            .startDate(LocalDate.of(2025, 6, 1))
            .build();
    JMembership m =
        JMembership.builder()
            .group(group)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 6, 1))
            .build();

    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(List.of(m));
    when(groupCourseRepository.findAllByGroupIdIn(any())).thenReturn(List.of(gc));

    CourseData result = service.resolveAllCoursesByLevels(STUDENT_ID);

    assertTrue(result.coursesByLevel().isEmpty());
  }

  private JExam exam(String coefficient, String examDate) {
    return JExam.builder()
        .id(EXAM_ID)
        .examDate(Instant.parse(examDate))
        .coefficient(new BigDecimal(coefficient))
        .groupCourse(
            JGroupCourse.builder()
                .id(GROUP_COURSE_ID)
                .group(JStudentGroup.builder().id(GROUP_ID).build())
                .build())
        .build();
  }

  private JGrade grade(JExam exam, float score) {
    return JGrade.builder().exam(exam).score(score).build();
  }

  private JMembership membership(LocalDate startDate, LocalDate endDate) {
    return JMembership.builder()
        .group(JStudentGroup.builder().id(GROUP_ID).promotion(promotion()).build())
        .startDate(startDate)
        .endDate(endDate)
        .build();
  }

  private JPromotion promotion() {
    return JPromotion.builder()
        .id(UUID.randomUUID())
        .reference("P-2024")
        .startYear((short) 2024)
        .endYear((short) 2025)
        .build();
  }
}
