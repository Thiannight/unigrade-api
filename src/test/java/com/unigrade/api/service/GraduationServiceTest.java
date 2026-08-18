package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.GraduationListEntry;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.Specialization;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.GradeCalculationService.CourseKey;
import com.unigrade.api.service.GradeCalculationService.CourseParticipation;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraduationServiceTest {

  private static final UUID PROMOTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID GROUP_COURSE_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID COURSE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final String STUDENT_ID = "STD00001";

  @Mock private PromotionRepository promotionRepository;
  @Mock private MembershipRepository membershipRepository;
  @Mock private GradeCalculationService gradeCalculationService;

  private GraduationService service;

  @BeforeEach
  void setUp() {
    service =
        new GraduationService(promotionRepository, membershipRepository, gradeCalculationService);
  }

  @Test
  void getGraduates_allCoursesAbove10_returnsStudent() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(student(Specialization.EL)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2024);
    stubFullCourses(STUDENT_ID);

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.getFirst().studentId());
    assertEquals("Alice", result.getFirst().firstName());
    assertEquals("Dupont", result.getFirst().lastName());
    assertEquals(1, result.getFirst().rank());
    assertEquals(new BigDecimal("12.00"), result.getFirst().allTimeAverage());
  }

  @Test
  void getGraduates_oneCourseBelow10_excludesStudent() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(student(Specialization.TN)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2024);
    stubResolvedCourses(STUDENT_ID);
    when(gradeCalculationService.isCourseComplete(GROUP_COURSE_ID, STUDENT_ID)).thenReturn(true);
    when(gradeCalculationService.courseAverage(GROUP_COURSE_ID, STUDENT_ID))
        .thenReturn(new BigDecimal("8.0"));

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.TN);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_nullSpecialization_throwsBadRequest() {
    assertThrows(BadRequestException.class, () -> service.getGraduates(PROMOTION_ID, null));
  }

  @Test
  void getGraduates_withFilter_returnsOnlyMatchingSpecialization() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(
            List.of(student(Specialization.EL), studentWithId("STD00002", Specialization.TN)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2024);
    stubFullCourses(STUDENT_ID);

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.getFirst().studentId());
  }

  @Test
  void getGraduates_noPromotion_throwsNotFound() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class, () -> service.getGraduates(PROMOTION_ID, Specialization.EL));
  }

  @Test
  void getGraduates_emptyPromotion_returnsEmptyList() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID)).thenReturn(List.of());

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_studentRetaken_excludedFromOldPromotion() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(student(Specialization.EL)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2026);

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_below180Credits_excludesStudent() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(student(Specialization.EL)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2024);
    stubResolvedCourses(STUDENT_ID);
    when(gradeCalculationService.isCourseComplete(GROUP_COURSE_ID, STUDENT_ID)).thenReturn(true);
    when(gradeCalculationService.courseAverage(GROUP_COURSE_ID, STUDENT_ID))
        .thenReturn(new BigDecimal("12.0"));
    // Course has 6 credits, but 180 required → excluded

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_rankSortedByAverageDescending() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(
            List.of(student(Specialization.EL), studentWithId("STD00002", Specialization.EL)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2024);
    when(membershipRepository.findLatestPromotionStartYearByStudentId("STD00002"))
        .thenReturn((short) 2024);
    stubFullCourses(STUDENT_ID);
    stubFullCourses("STD00002");
    // Override averages for ranking
    org.mockito.Mockito.lenient()
        .when(
            gradeCalculationService.courseAverage(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq(STUDENT_ID)))
        .thenReturn(new BigDecimal("12.0"));
    org.mockito.Mockito.lenient()
        .when(
            gradeCalculationService.courseAverage(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq("STD00002")))
        .thenReturn(new BigDecimal("16.0"));

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertEquals(2, result.size());
    assertEquals(1, result.get(0).rank());
    assertEquals("STD00002", result.get(0).studentId());
    assertEquals(2, result.get(1).rank());
    assertEquals(STUDENT_ID, result.get(1).studentId());
  }

  @Test
  void getGraduates_failedYearInOlderPromotion_discardedForGraduation() {
    JPromotion newPromo = promotion();
    UUID newGroupId = UUID.randomUUID();
    UUID newGcId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    JGroupCourse newGc =
        JGroupCourse.builder()
            .id(newGcId)
            .group(JStudentGroup.builder().id(newGroupId).promotion(newPromo).build())
            .course(
                JCourse.builder()
                    .id(courseId)
                    .reference("C1")
                    .title("C1")
                    .credits((short) 6)
                    .build())
            .build();

    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(newPromo));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(student(Specialization.EL)));
    when(membershipRepository.findLatestPromotionStartYearByStudentId(STUDENT_ID))
        .thenReturn((short) 2024);

    stubFullCourses(STUDENT_ID);
    // Override L1 to include the newGc course
    JStudentGroup newGroup = JStudentGroup.builder().id(newGroupId).promotion(newPromo).build();
    Map<CourseKey, CourseParticipation> l1Courses = new LinkedHashMap<>();
    l1Courses.put(
        new CourseKey(newPromo.getId(), courseId),
        new CourseParticipation(newGroup, newPromo, newGc));
    for (int i = 0; i < 9; i++) {
      UUID gcId = UUID.randomUUID();
      UUID cid = UUID.randomUUID();
      JGroupCourse gc =
          JGroupCourse.builder()
              .id(gcId)
              .group(newGroup)
              .course(
                  JCourse.builder()
                      .id(cid)
                      .reference("C-L1-" + i)
                      .title("Course L1 " + i)
                      .credits((short) 6)
                      .build())
              .build();
      l1Courses.put(
          new CourseKey(newPromo.getId(), cid), new CourseParticipation(newGroup, newPromo, gc));
    }
    org.mockito.Mockito.lenient()
        .when(gradeCalculationService.resolveCoursesByLevel(STUDENT_ID, Level.L1))
        .thenReturn(l1Courses);

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.getFirst().studentId());
  }

  private void stubResolvedCourses(String studentId) {
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
            .build();

    Map<CourseKey, CourseParticipation> deduped = new LinkedHashMap<>();
    deduped.put(new CourseKey(PROMOTION_ID, COURSE_ID), new CourseParticipation(group, promo, gc));
    for (Level level : Level.values()) {
      org.mockito.Mockito.lenient()
          .when(gradeCalculationService.resolveCoursesByLevel(studentId, level))
          .thenReturn(level == Level.L1 ? deduped : Map.of());
    }
  }

  private void stubFullCourses(String studentId) {
    JPromotion promo = promotion();
    JStudentGroup group = JStudentGroup.builder().id(GROUP_ID).promotion(promo).build();

    for (Level level : Level.values()) {
      Map<CourseKey, CourseParticipation> deduped = new LinkedHashMap<>();
      for (int i = 0; i < 10; i++) {
        UUID gcId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        JGroupCourse gc =
            JGroupCourse.builder()
                .id(gcId)
                .group(group)
                .course(
                    JCourse.builder()
                        .id(courseId)
                        .reference("C-" + level.name() + "-" + i)
                        .title("Course " + level.name() + " " + i)
                        .credits((short) 6)
                        .build())
                .build();
        deduped.put(
            new CourseKey(PROMOTION_ID, courseId), new CourseParticipation(group, promo, gc));
      }
      org.mockito.Mockito.lenient()
          .when(gradeCalculationService.resolveCoursesByLevel(studentId, level))
          .thenReturn(deduped);
    }

    org.mockito.Mockito.lenient()
        .when(
            gradeCalculationService.isCourseComplete(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq(studentId)))
        .thenReturn(true);
    org.mockito.Mockito.lenient()
        .when(
            gradeCalculationService.courseAverage(
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq(studentId)))
        .thenReturn(new BigDecimal("12.0"));
  }

  private JUser student(Specialization specialization) {
    return studentWithId(STUDENT_ID, specialization);
  }

  private JUser studentWithId(String id, Specialization specialization) {
    return JUser.builder()
        .id(id)
        .firstName("Alice")
        .lastName("Dupont")
        .role(com.unigrade.api.model.Role.STUDENT)
        .specialization(specialization)
        .build();
  }

  private JPromotion promotion() {
    return JPromotion.builder()
        .id(PROMOTION_ID)
        .reference("P-2024")
        .startYear((short) 2024)
        .endYear((short) 2025)
        .build();
  }
}
