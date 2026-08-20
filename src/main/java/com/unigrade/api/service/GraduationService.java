package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.GraduationListEntry;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.Specialization;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.GradeCalculationService.CourseData;
import com.unigrade.api.service.GradeCalculationService.CourseKey;
import com.unigrade.api.service.GradeCalculationService.CourseParticipation;
import com.unigrade.api.service.GradeCalculationService.CourseResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduationService {

  private final PromotionRepository promotionRepository;
  private final MembershipRepository membershipRepository;
  private final GradeCalculationService gradeCalculationService;

  public List<GraduationListEntry> getGraduates(UUID promotionId, Specialization specialization) {
    if (specialization == null) {
      throw new BadRequestException("Specialization is required");
    }
    JPromotion promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(() -> new NotFoundException("Promotion not found: " + promotionId));

    List<JUser> students = membershipRepository.findStudentsByPromotionId(promotionId);

    List<String> studentIds = students.stream().map(JUser::getId).toList();
    Map<String, Short> latestStartYears =
        membershipRepository.findLatestPromotionStartYearByStudentIds(studentIds).stream()
            .collect(
                Collectors.toMap(
                    MembershipRepository.LatestStartYearProjection::getStudentId,
                    MembershipRepository.LatestStartYearProjection::getStartYear));

    List<GraduationListEntry> entries = new ArrayList<>();
    for (JUser student : students) {
      if (student.getSpecialization() == null || student.getSpecialization() != specialization) {
        continue;
      }
      Short latestStartYear = latestStartYears.get(student.getId());
      if (latestStartYear != null && latestStartYear > promotion.getStartYear()) {
        continue;
      }
      var data = computeGraduationData(student.getId());
      if (data.allCoursesComplete
          && data.allCoursesPass
          && data.totalCredits >= Level.requiredCredits(Level.values().length)) {
        entries.add(
            new GraduationListEntry(
                0,
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                data.allTimeAverage));
      }
    }

    entries.sort(Comparator.comparing(GraduationListEntry::allTimeAverage).reversed());
    List<GraduationListEntry> ranked = new ArrayList<>();
    int rank = 1;
    for (GraduationListEntry g : entries) {
      ranked.add(
          new GraduationListEntry(
              rank++, g.studentId(), g.firstName(), g.lastName(), g.allTimeAverage()));
    }
    return ranked;
  }

  private GraduationData computeGraduationData(String studentId) {
    CourseData courseData = gradeCalculationService.resolveAllCoursesByLevels(studentId);

    boolean allComplete = true;
    boolean allPass = true;
    long earnedCredits = 0;
    BigDecimal weightedAverage = BigDecimal.ZERO;
    long averageCredits = 0;

    for (Level level : Level.values()) {
      Map<CourseKey, CourseParticipation> courses = courseData.coursesByLevel().get(level);
      if (courses == null) {
        continue;
      }
      for (CourseParticipation p : courses.values()) {
        UUID groupCourseId = p.groupCourse().getId();
        short credits = p.groupCourse().getCourse().getCredits();

        CourseResult result =
            gradeCalculationService.computeCourseResult(
                groupCourseId, studentId, courseData.memberships());

        if (!result.completed()) {
          allComplete = false;
        }

        BigDecimal average = result.average();
        if (average == null || average.compareTo(BigDecimal.TEN) < 0) {
          allPass = false;
        }

        if (average != null && average.compareTo(BigDecimal.TEN) >= 0) {
          earnedCredits += credits;
        }
        if (average != null) {
          weightedAverage = weightedAverage.add(average.multiply(BigDecimal.valueOf(credits)));
          averageCredits += credits;
        }
      }
    }

    BigDecimal allTimeAverage =
        averageCredits == 0
            ? null
            : weightedAverage.divide(BigDecimal.valueOf(averageCredits), 2, RoundingMode.HALF_UP);

    return new GraduationData(allComplete, allPass, earnedCredits, allTimeAverage);
  }

  private record GraduationData(
      boolean allCoursesComplete,
      boolean allCoursesPass,
      long totalCredits,
      BigDecimal allTimeAverage) {}
}
