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
import com.unigrade.api.service.GradeCalculationService.CourseKey;
import com.unigrade.api.service.GradeCalculationService.CourseParticipation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GraduationService {

  private static final BigDecimal TEN = BigDecimal.TEN;

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

    List<GraduationListEntry> entries = new ArrayList<>();
    for (JUser student : students) {
      if (student.getSpecialization() == null || student.getSpecialization() != specialization) {
        continue;
      }
      Short latestStartYear =
          membershipRepository.findLatestPromotionStartYearByStudentId(student.getId());
      if (latestStartYear != null && latestStartYear > promotion.getStartYear()) {
        continue;
      }
      if (isGraduated(student) && gradeCalculationService.totalCredits(student.getId()) >= 180) {
        entries.add(
            new GraduationListEntry(
                0,
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                gradeCalculationService.allTimeAverage(student.getId())));
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

  private boolean isGraduated(JUser student) {
    for (Level level : Level.values()) {
      Map<CourseKey, CourseParticipation> courses =
          gradeCalculationService.resolveCoursesByLevel(student.getId(), level);
      for (CourseParticipation participation : courses.values()) {
        BigDecimal average =
            gradeCalculationService.courseAverage(
                participation.groupCourse().getId(), student.getId());
        if (average == null || average.compareTo(TEN) < 0) {
          return false;
        }
      }
    }
    return true;
  }
}
