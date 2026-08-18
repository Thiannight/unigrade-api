package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.unigrade.api.model.ExamScore;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JStudentGroup;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
  void courseAverage_withGrades_returnsWeightedSum() {
    JExam exam = exam("1.0", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID, STUDENT_ID))
        .thenReturn(Optional.of(grade(14.0f)));

    BigDecimal result = service.courseAverage(GROUP_COURSE_ID, STUDENT_ID);

    assertEquals(0, new BigDecimal("14.0").compareTo(result));
  }

  @Test
  void courseAverage_noExams_returnsNull() {
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of());

    BigDecimal result = service.courseAverage(GROUP_COURSE_ID, STUDENT_ID);

    assertNull(result);
  }

  @Test
  void courseAverage_notMemberAtExamDate_skipsExam() {
    JExam exam = exam("1.0", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(false);

    BigDecimal result = service.courseAverage(GROUP_COURSE_ID, STUDENT_ID);

    assertNull(result);
  }

  @Test
  void courseAverage_noGrade_defaultsToZero() {
    JExam exam = exam("1.0", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID, STUDENT_ID))
        .thenReturn(Optional.empty());

    BigDecimal result = service.courseAverage(GROUP_COURSE_ID, STUDENT_ID);

    assertEquals(0, BigDecimal.ZERO.compareTo(result));
  }

  @Test
  void collectExamScores_withGrade_returnsScoreList() {
    JExam exam = exam("0.5", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID, STUDENT_ID))
        .thenReturn(Optional.of(grade(15.0f)));

    List<ExamScore> result = service.collectExamScores(GROUP_COURSE_ID, STUDENT_ID);

    assertEquals(1, result.size());
    assertEquals(new BigDecimal("15.0"), result.getFirst().score());
    assertEquals(new BigDecimal("0.5"), result.getFirst().coefficient());
  }

  @Test
  void collectExamScores_notMember_excludesExam() {
    JExam exam = exam("0.5", "2024-05-01T09:00:00Z");
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(false);

    List<ExamScore> result = service.collectExamScores(GROUP_COURSE_ID, STUDENT_ID);

    assertTrue(result.isEmpty());
  }

  @Test
  void allTimeAverage_noMemberships_returnsNull() {
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID)).thenReturn(List.of());

    BigDecimal result = service.allTimeAverage(STUDENT_ID);

    assertNull(result);
  }

  @Test
  void totalCredits_withCourses_returnsSum() {
    UUID groupCourseId2 = UUID.randomUUID();
    JGroupCourse gc1 = groupCourse(GROUP_ID, GROUP_COURSE_ID, 6);
    JGroupCourse gc2 = groupCourse(GROUP_ID, groupCourseId2, 4);
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(List.of(membership()));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID)).thenReturn(List.of(gc1, gc2));
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID))
        .thenReturn(List.of(exam("1.0", "2024-05-01T09:00:00Z")));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID, STUDENT_ID))
        .thenReturn(Optional.of(grade(14.0f)));
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(groupCourseId2))
        .thenReturn(List.of());

    long result = service.totalCredits(STUDENT_ID);

    assertEquals(6, result);
  }

  @Test
  void totalCredits_noMemberships_returnsZero() {
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID)).thenReturn(List.of());

    long result = service.totalCredits(STUDENT_ID);

    assertEquals(0, result);
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

  private JGrade grade(float score) {
    return JGrade.builder().score(score).build();
  }

  private JGroupCourse groupCourse(UUID groupId, UUID groupCourseId, int credits) {
    return JGroupCourse.builder()
        .id(groupCourseId)
        .group(JStudentGroup.builder().id(groupId).promotion(promotion()).build())
        .course(
            com.unigrade.api.repository.model.JCourse.builder()
                .id(UUID.randomUUID())
                .reference("C-REF")
                .title("Course")
                .credits((short) credits)
                .build())
        .semester(com.unigrade.api.model.Semester.S1)
        .startDate(LocalDate.of(2024, 1, 1))
        .build();
  }

  private com.unigrade.api.repository.model.JMembership membership() {
    return com.unigrade.api.repository.model.JMembership.builder()
        .group(JStudentGroup.builder().id(GROUP_ID).promotion(promotion()).build())
        .startDate(LocalDate.of(2024, 1, 1))
        .build();
  }

  private com.unigrade.api.repository.model.JPromotion promotion() {
    return com.unigrade.api.repository.model.JPromotion.builder()
        .id(UUID.randomUUID())
        .reference("P-2024")
        .startYear((short) 2024)
        .endYear((short) 2025)
        .build();
  }
}
