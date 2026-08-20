package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
import com.unigrade.api.service.GradeCalculationService.CourseData;
import com.unigrade.api.service.GradeCalculationService.CourseKey;
import com.unigrade.api.service.GradeCalculationService.CourseParticipation;
import com.unigrade.api.service.GradeCalculationService.CourseResult;
import java.math.BigDecimal;
import java.util.EnumMap;
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
    stubLatestStartYear(STUDENT_ID, (short) 2024);
    stubFullCourses(STUDENT_ID);
    when(gradeCalculationService.computeCourseResult(any(UUID.class), eq(STUDENT_ID), any()))
        .thenReturn(new CourseResult(true, new BigDecimal("12.0"), List.of()));

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
    stubLatestStartYear(STUDENT_ID, (short) 2024);
    stubCoursesWithAverages(STUDENT_ID, new BigDecimal("8.0"));

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.TN);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_withFilter_returnsOnlyMatchingSpecialization() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(
            List.of(student(Specialization.EL), studentWithId("STD00002", Specialization.TN)));
    stubLatestStartYear(STUDENT_ID, (short) 2024);
    stubFullCourses(STUDENT_ID);
    when(gradeCalculationService.computeCourseResult(any(UUID.class), eq(STUDENT_ID), any()))
        .thenReturn(new CourseResult(true, new BigDecimal("12.0"), List.of()));

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
    stubLatestStartYear(STUDENT_ID, (short) 2026);

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_below180Credits_excludesStudent() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(List.of(student(Specialization.EL)));
    stubLatestStartYear(STUDENT_ID, (short) 2024);
    stubCoursesWithAverages(STUDENT_ID, new BigDecimal("12.0"));

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertTrue(result.isEmpty());
  }

  @Test
  void getGraduates_rankSortedByAverageDescending() {
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(membershipRepository.findStudentsByPromotionId(PROMOTION_ID))
        .thenReturn(
            List.of(student(Specialization.EL), studentWithId("STD00002", Specialization.EL)));
    stubLatestStartYear(STUDENT_ID, (short) 2024);
    stubLatestStartYear("STD00002", (short) 2024);
    stubFullCourses(STUDENT_ID);
    stubFullCourses("STD00002");
    when(gradeCalculationService.computeCourseResult(any(UUID.class), eq(STUDENT_ID), any()))
        .thenReturn(new CourseResult(true, new BigDecimal("12.0"), List.of()));
    when(gradeCalculationService.computeCourseResult(any(UUID.class), eq("STD00002"), any()))
        .thenReturn(new CourseResult(true, new BigDecimal("16.0"), List.of()));

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertEquals(2, result.size());
    assertEquals(1, result.getFirst().rank());
    assertEquals("STD00002", result.getFirst().studentId());
    assertEquals(2, result.get(1).rank());
    assertEquals(STUDENT_ID, result.get(1).studentId());
  }

  @Test
  void getGraduates_failedYearInOlderPromotion_discardedForGraduation() {
    JPromotion newPromo = promotion();
    UUID newGroupId = UUID.randomUUID();
    UUID newGcId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    JStudentGroup newGroup = JStudentGroup.builder().id(newGroupId).promotion(newPromo).build();
    JGroupCourse newGc =
        JGroupCourse.builder()
            .id(newGcId)
            .group(newGroup)
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
    stubLatestStartYear(STUDENT_ID, (short) 2024);

    JPromotion promo = promotion();
    JStudentGroup group = JStudentGroup.builder().id(GROUP_ID).promotion(promo).build();
    Map<Level, Map<CourseKey, CourseParticipation>> coursesByLevel = new EnumMap<>(Level.class);

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
    coursesByLevel.put(Level.L1, l1Courses);

    for (Level level : List.of(Level.L2, Level.L3)) {
      Map<CourseKey, CourseParticipation> courses = new LinkedHashMap<>();
      for (int i = 0; i < 10; i++) {
        UUID gcId = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        JGroupCourse gc =
            JGroupCourse.builder()
                .id(gcId)
                .group(group)
                .course(
                    JCourse.builder()
                        .id(cid)
                        .reference("C-" + level.name() + "-" + i)
                        .title("Course " + level.name() + " " + i)
                        .credits((short) 6)
                        .build())
                .build();
        courses.put(new CourseKey(promo.getId(), cid), new CourseParticipation(group, promo, gc));
      }
      coursesByLevel.put(level, courses);
    }

    when(gradeCalculationService.resolveAllCoursesByLevels(STUDENT_ID))
        .thenReturn(new CourseData(coursesByLevel, List.of()));
    when(gradeCalculationService.computeCourseResult(any(UUID.class), eq(STUDENT_ID), any()))
        .thenReturn(new CourseResult(true, new BigDecimal("12.0"), List.of()));

    List<GraduationListEntry> result = service.getGraduates(PROMOTION_ID, Specialization.EL);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.getFirst().studentId());
  }

  private void stubLatestStartYear(String studentId, short startYear) {
    when(membershipRepository.findLatestPromotionStartYearByStudentIds(any()))
        .thenReturn(
            List.of(
                new MembershipRepository.LatestStartYearProjection() {
                  @Override
                  public String getStudentId() {
                    return studentId;
                  }

                  @Override
                  public Short getStartYear() {
                    return startYear;
                  }
                }));
  }

  private void stubCoursesWithAverages(String studentId, BigDecimal average) {
    JPromotion promo = promotion();
    JStudentGroup group = JStudentGroup.builder().id(GROUP_ID).promotion(promo).build();

    Map<Level, Map<CourseKey, CourseParticipation>> coursesByLevel = new EnumMap<>(Level.class);
    for (Level level : Level.values()) {
      Map<CourseKey, CourseParticipation> courses = new LinkedHashMap<>();
      UUID gcId = UUID.randomUUID();
      UUID cid = UUID.randomUUID();
      JGroupCourse gc =
          JGroupCourse.builder()
              .id(gcId)
              .group(group)
              .course(
                  JCourse.builder()
                      .id(cid)
                      .reference("C-" + level.name())
                      .title("Course " + level.name())
                      .credits((short) 6)
                      .build())
              .build();
      courses.put(new CourseKey(PROMOTION_ID, cid), new CourseParticipation(group, promo, gc));
      coursesByLevel.put(level, courses);
    }

    when(gradeCalculationService.resolveAllCoursesByLevels(studentId))
        .thenReturn(new CourseData(coursesByLevel, List.of()));
    when(gradeCalculationService.computeCourseResult(any(UUID.class), eq(studentId), any()))
        .thenReturn(new CourseResult(true, average, List.of()));
  }

  private void stubFullCourses(String studentId) {
    JPromotion promo = promotion();
    JStudentGroup group = JStudentGroup.builder().id(GROUP_ID).promotion(promo).build();

    Map<Level, Map<CourseKey, CourseParticipation>> coursesByLevel = new EnumMap<>(Level.class);
    for (Level level : Level.values()) {
      Map<CourseKey, CourseParticipation> deduped = new LinkedHashMap<>();
      for (int i = 0; i < 10; i++) {
        UUID gcId = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        JGroupCourse gc =
            JGroupCourse.builder()
                .id(gcId)
                .group(group)
                .course(
                    JCourse.builder()
                        .id(cid)
                        .reference("C-" + level.name() + "-" + i)
                        .title("Course " + level.name() + " " + i)
                        .credits((short) 6)
                        .build())
                .build();
        deduped.put(new CourseKey(PROMOTION_ID, cid), new CourseParticipation(group, promo, gc));
      }
      coursesByLevel.put(level, deduped);
    }

    when(gradeCalculationService.resolveAllCoursesByLevels(studentId))
        .thenReturn(new CourseData(coursesByLevel, List.of()));
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
