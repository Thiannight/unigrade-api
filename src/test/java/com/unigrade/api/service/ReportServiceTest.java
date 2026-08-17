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
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.Semester;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  @Mock private MembershipRepository membershipRepository;
  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private ExamRepository examRepository;
  @Mock private GradeRepository gradeRepository;

  private ReportService service;

  @BeforeEach
  void setUp() {
    service =
        new ReportService(
            userRepository,
            membershipRepository,
            groupCourseRepository,
            examRepository,
            gradeRepository);
  }

  @Test
  void generate_happyPath_buildsLevelReport() {
    JUser student = student();
    JGroupCourse groupCourse =
        groupCourse(GROUP_COURSE_ID_1, GROUP_ID_1, Semester.S3, promotion("P-2024", (short) 2024));
    JExam exam1 = exam(EXAM_ID_1, "2024-05-01T09:00:00Z", "0.4");
    JExam exam2 = exam(EXAM_ID_2, "2024-06-01T09:00:00Z", "0.6");
    LocalDate examDate1 = exam1.getExamDate().atOffset(ZoneOffset.UTC).toLocalDate();
    LocalDate examDate2 = exam2.getExamDate().atOffset(ZoneOffset.UTC).toLocalDate();

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(List.of(membership(GROUP_ID_1, student, promotion("P-2024", (short) 2024))));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_1)).thenReturn(List.of(groupCourse));
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID_1))
        .thenReturn(List.of(exam1, exam2));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(GROUP_ID_1, STUDENT_ID, examDate1))
        .thenReturn(true);
    when(membershipRepository.existsByGroupIdAndStudentIdAt(GROUP_ID_1, STUDENT_ID, examDate2))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID_1, STUDENT_ID))
        .thenReturn(Optional.of(grade(10.0f)));
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID_2, STUDENT_ID))
        .thenReturn(Optional.of(grade(16.0f)));

    StudentReport report = service.generate(STUDENT_ID);

    assertEquals(STUDENT_ID, report.studentId());
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
    assertEquals(new BigDecimal("13.60"), report.overallAverage());
  }

  @Test
  void generate_missingStudent_throwsNotFound() {
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.generate(STUDENT_ID));
  }

  @Test
  void generate_nonStudent_throwsBadRequest() {
    JUser teacher = JUser.builder().id(STUDENT_ID).role(Role.TEACHER).build();
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(teacher));

    assertThrows(BadRequestException.class, () -> service.generate(STUDENT_ID));
  }

  @Test
  void generate_noMemberships_returnsEmptyReport() {
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID)).thenReturn(List.of());

    StudentReport report = service.generate(STUDENT_ID);

    assertTrue(report.levels().isEmpty());
    assertNull(report.overallAverage());
  }

  @Test
  void generate_repeat_keepsOnlyLatestPromotionPerLevel() {
    JUser student = student();
    JGroupCourse oldCourse =
        groupCourse(GROUP_COURSE_ID_1, GROUP_ID_1, Semester.S3, promotion("P-2022", (short) 2022));
    JGroupCourse newCourse =
        groupCourse(GROUP_COURSE_ID_2, GROUP_ID_2, Semester.S3, promotion("P-2024", (short) 2024));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(
            List.of(
                membership(GROUP_ID_1, student, promotion("P-2022", (short) 2022)),
                membership(GROUP_ID_2, student, promotion("P-2024", (short) 2024))));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_1)).thenReturn(List.of(oldCourse));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_2)).thenReturn(List.of(newCourse));

    StudentReport report = service.generate(STUDENT_ID);

    assertEquals(1, report.levels().size());
    assertEquals(1, report.levels().getFirst().courses().size());
    assertEquals("P-2024", report.levels().getFirst().courses().getFirst().promotionReference());
  }

  @Test
  void generate_excludesExamWhenNotMemberAtExamDate() {
    JUser student = student();
    JGroupCourse groupCourse =
        groupCourse(GROUP_COURSE_ID_1, GROUP_ID_1, Semester.S1, promotion("P-2024", (short) 2024));
    JExam exam = exam(EXAM_ID_1, "2024-05-01T09:00:00Z", "0.5");
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(List.of(membership(GROUP_ID_1, student, promotion("P-2024", (short) 2024))));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_1)).thenReturn(List.of(groupCourse));
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID_1))
        .thenReturn(List.of(exam));

    StudentReport report = service.generate(STUDENT_ID);

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
    JGroupCourse firstAssignment =
        groupCourse(GROUP_COURSE_ID_1, GROUP_ID_1, Semester.S4, promotion("P-2024", (short) 2024));
    JGroupCourse secondAssignment =
        groupCourse(GROUP_COURSE_ID_2, GROUP_ID_2, Semester.S4, promotion("P-2024", (short) 2024));
    JExam exam1 = exam(EXAM_ID_1, "2024-05-01T09:00:00Z", "0.5");
    JExam exam2 = exam(EXAM_ID_2, "2024-06-01T09:00:00Z", "0.5");

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(
            List.of(
                membership(GROUP_ID_1, student, promotion("P-2024", (short) 2024)),
                membership(GROUP_ID_2, student, promotion("P-2024", (short) 2024))));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_1)).thenReturn(List.of(firstAssignment));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_2)).thenReturn(List.of(secondAssignment));
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID_1))
        .thenReturn(List.of(exam1));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID_1), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(EXAM_ID_1, STUDENT_ID))
        .thenReturn(Optional.of(grade(10.0f)));

    StudentReport report = service.generate(STUDENT_ID);

    assertEquals(1, report.levels().size());
    assertEquals(1, report.levels().getFirst().courses().size());
    var course = report.levels().getFirst().courses().getFirst();
    assertEquals(1, course.exams().size());
    assertEquals(new BigDecimal("10.0"), course.exams().getFirst().score());
  }

  @Test
  void generate_transferWithinPromotion_newGroupCourseAppears() {
    JUser student = student();
    UUID courseIdOld = UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA");
    UUID courseIdNew = UUID.fromString("BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB");
    UUID examIdNew = UUID.fromString("CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC");
    JGroupCourse g1Course =
        JGroupCourse.builder()
            .id(GROUP_COURSE_ID_1)
            .group(group(GROUP_ID_1, promotion("P-2024", (short) 2024)))
            .course(
                JCourse.builder()
                    .id(courseIdOld)
                    .reference("C-OLD")
                    .title("Old")
                    .credits((short) 6)
                    .build())
            .semester(Semester.S4)
            .build();
    JGroupCourse g2Course =
        JGroupCourse.builder()
            .id(GROUP_COURSE_ID_2)
            .group(group(GROUP_ID_2, promotion("P-2024", (short) 2024)))
            .course(
                JCourse.builder()
                    .id(courseIdNew)
                    .reference("C-NEW")
                    .title("New")
                    .credits((short) 6)
                    .build())
            .semester(Semester.S4)
            .build();
    JExam examNew = exam(examIdNew, "2024-07-01T09:00:00Z", "1.0");

    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    when(membershipRepository.findByStudentIdOrderByStartDateAsc(STUDENT_ID))
        .thenReturn(
            List.of(
                membership(GROUP_ID_1, student, promotion("P-2024", (short) 2024)),
                membership(GROUP_ID_2, student, promotion("P-2024", (short) 2024))));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_1)).thenReturn(List.of(g1Course));
    when(groupCourseRepository.findAllByGroupId(GROUP_ID_2)).thenReturn(List.of(g2Course));
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID_1))
        .thenReturn(List.of());
    when(examRepository.findByGroupCourseIdOrderByExamDateAsc(GROUP_COURSE_ID_2))
        .thenReturn(List.of(examNew));
    when(membershipRepository.existsByGroupIdAndStudentIdAt(
            eq(GROUP_ID_2), eq(STUDENT_ID), any(LocalDate.class)))
        .thenReturn(true);
    when(gradeRepository.findTopByExamIdAndStudentIdOrderByGradeDateDesc(examIdNew, STUDENT_ID))
        .thenReturn(Optional.of(grade(14.0f)));

    StudentReport report = service.generate(STUDENT_ID);

    assertEquals(1, report.levels().size());
    var courses = report.levels().getFirst().courses();
    assertEquals(2, courses.size());
    assertEquals(courseIdOld, courses.get(0).courseId());
    assertEquals(courseIdNew, courses.get(1).courseId());
    assertEquals(new BigDecimal("14.00"), courses.get(1).average());
  }

  private JUser student() {
    return JUser.builder().id(STUDENT_ID).role(Role.STUDENT).build();
  }

  private JMembership membership(UUID groupId, JUser student, JPromotion promotion) {
    return JMembership.builder()
        .id(UUID.randomUUID())
        .group(group(groupId, promotion))
        .student(student)
        .startDate(LocalDate.of(2024, 1, 1))
        .endDate(null)
        .build();
  }

  private JPromotion promotion(String reference, Short startYear) {
    return JPromotion.builder()
        .id(PROMOTION_ID_1)
        .reference(reference)
        .startYear(startYear)
        .endYear((short) (startYear + 1))
        .build();
  }

  private JStudentGroup group(UUID id, JPromotion promotion) {
    return JStudentGroup.builder().id(id).reference("A1").promotion(promotion).build();
  }

  private JGroupCourse groupCourse(
      UUID groupCourseId, UUID groupId, Semester semester, JPromotion promotion) {
    return JGroupCourse.builder()
        .id(groupCourseId)
        .group(group(groupId, promotion))
        .course(
            JCourse.builder()
                .id(COURSE_ID)
                .reference("C-REF")
                .title("Course")
                .credits((short) 6)
                .build())
        .semester(semester)
        .build();
  }

  private JExam exam(UUID id, String examDate, String coefficient) {
    return JExam.builder()
        .id(id)
        .examDate(Instant.parse(examDate))
        .coefficient(new BigDecimal(coefficient))
        .build();
  }

  private JGrade grade(float score) {
    return JGrade.builder().score(score).build();
  }
}
