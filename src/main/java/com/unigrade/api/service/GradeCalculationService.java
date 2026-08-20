package com.unigrade.api.service;

import com.unigrade.api.model.ExamScore;
import com.unigrade.api.model.Level;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GradeCalculationService {

  private final MembershipRepository membershipRepository;
  private final GroupCourseRepository groupCourseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;

  public record CourseResult(boolean completed, BigDecimal average, List<ExamScore> exams) {}

  public record CourseParticipation(
      JStudentGroup group, JPromotion promotion, JGroupCourse groupCourse) {}

  public record CourseKey(UUID promotionId, UUID courseId) {}

  public record CourseData(
      Map<Level, Map<CourseKey, CourseParticipation>> coursesByLevel,
      List<JMembership> memberships) {}

  public CourseData resolveAllCoursesByLevels(String studentId) {
    List<JMembership> memberships =
        membershipRepository.findByStudentIdOrderByStartDateAsc(studentId);

    Map<Level, List<CourseParticipation>> participationsByLevel =
        collectCourseParticipations(memberships);

    Map<Level, Map<CourseKey, CourseParticipation>> result = new EnumMap<>(Level.class);
    for (Map.Entry<Level, List<CourseParticipation>> entry : participationsByLevel.entrySet()) {
      result.put(entry.getKey(), deduplicateByCourse(keepLatestPromotion(entry.getValue())));
    }
    return new CourseData(result, memberships);
  }

  public CourseResult computeCourseResult(
      UUID groupCourseId, String studentId, List<JMembership> memberships) {
    List<ExamScore> scores = collectExamScores(groupCourseId, studentId, memberships);
    boolean complete =
        scores.stream()
                .map(ExamScore::coefficient)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(BigDecimal.ONE)
            == 0;
    return new CourseResult(complete, averageFromExamScores(scores), scores);
  }

  private List<ExamScore> collectExamScores(
      UUID groupCourseId, String studentId, List<JMembership> memberships) {
    List<JExam> exams = examRepository.findByGroupCourseIdOrderByExamDateAsc(groupCourseId);
    if (exams.isEmpty()) {
      return List.of();
    }
    UUID groupId = exams.getFirst().getGroupCourse().getGroup().getId();

    List<UUID> examIds = exams.stream().map(JExam::getId).toList();
    Map<UUID, JGrade> latestExamGrades = findLatestGrades(studentId, examIds);

    List<ExamScore> result = new ArrayList<>();
    for (JExam exam : exams) {
      LocalDate examDate = exam.getExamDate().atOffset(ZoneOffset.UTC).toLocalDate();
      if (!isMemberAt(memberships, groupId, examDate)) {
        continue;
      }

      BigDecimal score =
          latestExamGrades.containsKey(exam.getId())
              ? BigDecimal.valueOf(latestExamGrades.get(exam.getId()).getScore())
              : BigDecimal.ZERO;
      result.add(new ExamScore(exam.getId(), exam.getExamDate(), exam.getCoefficient(), score));
    }
    return result;
  }

  private Map<UUID, JGrade> findLatestGrades(String studentId, List<UUID> examIds) {
    if (examIds.isEmpty()) {
      return Map.of();
    }

    List<JGrade> grades =
        gradeRepository.findByStudentIdAndExamIdsOrderByGradeDateDesc(studentId, examIds);
    Map<UUID, JGrade> latest = new HashMap<>();
    for (JGrade grade : grades) {
      latest.putIfAbsent(grade.getExam().getId(), grade);
    }
    return latest;
  }

  private boolean isMemberAt(List<JMembership> memberships, UUID groupId, LocalDate date) {
    for (JMembership m : memberships) {
      if (m.getGroup().getId().equals(groupId)
          && !m.getStartDate().isAfter(date)
          && (m.getEndDate() == null || !m.getEndDate().isBefore(date))) {
        return true;
      }
    }
    return false;
  }

  public Map<Level, List<CourseParticipation>> collectCourseParticipations(
      List<JMembership> memberships) {
    Set<UUID> groupIds =
        memberships.stream().map(m -> m.getGroup().getId()).collect(Collectors.toSet());

    Map<UUID, List<JGroupCourse>> coursesByGroup =
        groupCourseRepository.findAllByGroupIdIn(groupIds).stream()
            .collect(Collectors.groupingBy(gc -> gc.getGroup().getId()));

    Map<Level, List<CourseParticipation>> participationsByLevel = new EnumMap<>(Level.class);
    for (JMembership membership : memberships) {
      JStudentGroup group = membership.getGroup();
      JPromotion promotion = group.getPromotion();

      List<JGroupCourse> groupCourses = coursesByGroup.getOrDefault(group.getId(), List.of());
      for (JGroupCourse groupCourse : groupCourses) {
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
}
