package com.unigrade.api.service;

import com.unigrade.api.model.ExamScore;
import com.unigrade.api.model.Level;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GradeCalculationService {

  private final MembershipRepository membershipRepository;
  private final GroupCourseRepository groupCourseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;

  public List<ExamScore> collectExamScores(UUID groupCourseId, String studentId) {
    List<JExam> exams = examRepository.findByGroupCourseIdOrderByExamDateAsc(groupCourseId);
    if (exams.isEmpty()) {
      return List.of();
    }
    UUID groupId = exams.getFirst().getGroupCourse().getGroup().getId();
    List<ExamScore> result = new ArrayList<>();
    for (JExam exam : exams) {
      LocalDate examDate = exam.getExamDate().atOffset(ZoneOffset.UTC).toLocalDate();
      if (!membershipRepository.existsByGroupIdAndStudentIdAt(groupId, studentId, examDate)) {
        continue;
      }
      BigDecimal score =
          gradeRepository
              .findTopByExamIdAndStudentIdOrderByGradeDateDesc(exam.getId(), studentId)
              .map(g -> BigDecimal.valueOf(g.getScore()))
              .orElse(BigDecimal.ZERO);
      result.add(new ExamScore(exam.getId(), exam.getExamDate(), exam.getCoefficient(), score));
    }
    return result;
  }

  public BigDecimal courseAverage(UUID groupCourseId, String studentId) {
    return averageFromExamScores(collectExamScores(groupCourseId, studentId));
  }

  public Map<CourseKey, CourseParticipation> resolveCoursesByLevel(String studentId, Level level) {
    List<JMembership> memberships =
        membershipRepository.findByStudentIdOrderByStartDateAsc(studentId);
    Map<Level, List<CourseParticipation>> participationByLevel =
        collectCourseParticipations(memberships);
    List<CourseParticipation> participations = participationByLevel.get(level);
    if (participations == null) {
      return Map.of();
    }
    return deduplicateByCourse(keepLatestPromotion(participations));
  }

  public long totalCredits(String studentId) {
    long total = 0;
    for (Level level : Level.values()) {
      for (CourseParticipation p : resolveCoursesByLevel(studentId, level).values()) {
        if (courseAverage(p.groupCourse().getId(), studentId) != null) {
          total += p.groupCourse().getCourse().getCredits();
        }
      }
    }
    return total;
  }

  public BigDecimal allTimeAverage(String studentId) {
    BigDecimal weighted = BigDecimal.ZERO;
    long totalCredits = 0;
    for (Level level : Level.values()) {
      for (CourseParticipation p : resolveCoursesByLevel(studentId, level).values()) {
        BigDecimal avg = courseAverage(p.groupCourse().getId(), studentId);
        if (avg != null) {
          weighted =
              weighted.add(
                  avg.multiply(BigDecimal.valueOf(p.groupCourse().getCourse().getCredits())));
          totalCredits += p.groupCourse().getCourse().getCredits();
        }
      }
    }
    if (totalCredits == 0) {
      return null;
    }
    return weighted.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);
  }

  public Map<Level, List<CourseParticipation>> collectCourseParticipations(
      List<JMembership> memberships) {
    Map<Level, List<CourseParticipation>> participationsByLevel = new EnumMap<>(Level.class);
    for (JMembership membership : memberships) {
      JStudentGroup group = membership.getGroup();
      JPromotion promotion = group.getPromotion();
      for (JGroupCourse groupCourse : groupCourseRepository.findAllByGroupId(group.getId())) {
        if ((membership.getEndDate() != null
                && groupCourse.getStartDate().isAfter(membership.getEndDate()))
            || (groupCourse.getEndDate() != null
                && membership.getStartDate().isAfter(groupCourse.getEndDate()))) {
          continue;
        }
        participationsByLevel
            .computeIfAbsent(groupCourse.getSemester().level(), level -> new ArrayList<>())
            .add(new CourseParticipation(group, promotion, groupCourse));
      }
    }
    return participationsByLevel;
  }

  public List<CourseParticipation> keepLatestPromotion(List<CourseParticipation> participations) {
    if (participations.isEmpty()) return new ArrayList<>();
    short maxStartYear = participations.getFirst().promotion().getStartYear();
    for (CourseParticipation participation : participations) {
      if (participation.promotion().getStartYear() > maxStartYear) {
        maxStartYear = participation.promotion().getStartYear();
      }
    }
    short latestStartYear = maxStartYear;
    return participations.stream()
        .filter(participation -> participation.promotion().getStartYear() == latestStartYear)
        .toList();
  }

  public Map<CourseKey, CourseParticipation> deduplicateByCourse(
      List<CourseParticipation> participations) {
    Map<CourseKey, CourseParticipation> result = new LinkedHashMap<>();
    for (CourseParticipation participation : participations) {
      result.putIfAbsent(
          new CourseKey(
              participation.promotion().getId(), participation.groupCourse().getCourse().getId()),
          participation);
    }
    return result;
  }

  public BigDecimal averageFromExamScores(List<ExamScore> scores) {
    if (scores.isEmpty()) {
      return null;
    }
    BigDecimal weighted = BigDecimal.ZERO;
    BigDecimal totalCoefficient = BigDecimal.ZERO;
    for (ExamScore score : scores) {
      weighted = weighted.add(score.score().multiply(score.coefficient()));
      totalCoefficient = totalCoefficient.add(score.coefficient());
    }
    if (totalCoefficient.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return weighted;
  }

  public record CourseParticipation(
      JStudentGroup group, JPromotion promotion, JGroupCourse groupCourse) {}

  public record CourseKey(UUID promotionId, UUID courseId) {}
}
