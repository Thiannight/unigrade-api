package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.CourseReportEntry;
import com.unigrade.api.model.ExamScore;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.LevelReport;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.SecurityUtils;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final int PER_LEVEL_CREDIT = 60;

  private final UserRepository userRepository;
  private final MembershipRepository membershipRepository;
  private final GroupCourseRepository groupCourseRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;

  @Transactional(readOnly = true)
  public StudentReport generate(String studentId, Level levelFilter) {
    requireCanViewReport(studentId);
    JUser student = resolveStudent(studentId);

    List<JMembership> memberships =
        membershipRepository.findByStudentIdOrderByStartDateAsc(studentId);
    Map<Level, List<CourseParticipation>> participationsByLevel =
        collectCourseParticipations(memberships);

    List<LevelReport> levelReports = new ArrayList<>();
    for (Level level : Level.values()) {
      if (levelFilter != null && level != levelFilter) {
        continue;
      }
      List<CourseParticipation> participations = participationsByLevel.get(level);
      if (participations == null) {
        continue;
      }
      LevelReport levelReport =
          buildLevelReport(level, keepLatestPromotion(participations), studentId);
      levelReports.add(levelReport);
    }

    List<CourseReportEntry> allCourses =
        levelReports.stream().flatMap(lr -> lr.courses().stream()).toList();
    ReportStatus status =
        levelReports.stream().anyMatch(lr -> lr.status() == ReportStatus.TEMPORARY)
            ? ReportStatus.TEMPORARY
            : ReportStatus.COMPLETE;

    long totalCredits = levelReports.stream().mapToLong(LevelReport::totalCredits).sum();
    long requiredCredits = levelReports.stream().mapToLong(LevelReport::requiredCredits).sum();

    return new StudentReport(
        studentId,
        student.getFirstName(),
        student.getLastName(),
        status,
        totalCredits,
        requiredCredits,
        levelReports,
        average(allCourses));
  }

  private Map<Level, List<CourseParticipation>> collectCourseParticipations(
      List<JMembership> memberships) {
    Map<Level, List<CourseParticipation>> participationsByLevel = new EnumMap<>(Level.class);
    for (JMembership membership : memberships) {
      JStudentGroup group = membership.getGroup();
      JPromotion promotion = group.getPromotion();
      for (JGroupCourse groupCourse : groupCourseRepository.findAllByGroupId(group.getId())) {
        participationsByLevel
            .computeIfAbsent(groupCourse.getSemester().level(), level -> new ArrayList<>())
            .add(new CourseParticipation(group, promotion, groupCourse));
      }
    }
    return participationsByLevel;
  }

  private List<CourseParticipation> keepLatestPromotion(List<CourseParticipation> participations) {
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

  private LevelReport buildLevelReport(
      Level level, List<CourseParticipation> participations, String studentId) {
    Map<CourseKey, CourseParticipation> participationsByCourse = new LinkedHashMap<>();
    for (CourseParticipation participation : participations) {
      participationsByCourse.putIfAbsent(
          new CourseKey(
              participation.promotion().getId(), participation.groupCourse().getCourse().getId()),
          participation);
    }

    List<CourseReportEntry> courses = new ArrayList<>();
    for (Map.Entry<CourseKey, CourseParticipation> entry : participationsByCourse.entrySet()) {
      CourseParticipation participation = entry.getValue();
      JGroupCourse representative = participation.groupCourse();
      String promotionReference = participation.promotion().getReference();
      List<ExamScore> exams = collectExams(participation, studentId);
      boolean completed =
          exams.stream()
                  .map(ExamScore::coefficient)
                  .reduce(BigDecimal.ZERO, BigDecimal::add)
                  .compareTo(ONE)
              == 0;
      courses.add(
          new CourseReportEntry(
              representative.getCourse().getId(),
              promotionReference,
              representative.getCourse().getReference(),
              representative.getCourse().getTitle(),
              representative.getCourse().getCredits(),
              completed,
              averageExams(exams),
              exams));
    }
    boolean allCompleted = courses.stream().allMatch(CourseReportEntry::completed);
    long totalCredits = courses.stream().mapToLong(CourseReportEntry::credits).sum();
    ReportStatus status =
        allCompleted && totalCredits >= PER_LEVEL_CREDIT
            ? ReportStatus.COMPLETE
            : ReportStatus.TEMPORARY;
    return new LevelReport(
        level, status, totalCredits, PER_LEVEL_CREDIT, average(courses), courses);
  }

  private List<ExamScore> collectExams(CourseParticipation participation, String studentId) {
    List<ExamScore> exams = new ArrayList<>();
    for (JExam exam :
        examRepository.findByGroupCourseIdOrderByExamDateAsc(participation.groupCourse().getId())) {
      LocalDate examDate = exam.getExamDate().atOffset(ZoneOffset.UTC).toLocalDate();
      if (!membershipRepository.existsByGroupIdAndStudentIdAt(
          participation.group().getId(), studentId, examDate)) {
        continue;
      }
      exams.add(toExamScore(exam, studentId));
    }
    return exams;
  }

  private ExamScore toExamScore(JExam exam, String studentId) {
    BigDecimal score =
        gradeRepository
            .findTopByExamIdAndStudentIdOrderByGradeDateDesc(exam.getId(), studentId)
            .map(grade -> BigDecimal.valueOf(grade.getScore()))
            .orElse(BigDecimal.ZERO);
    return new ExamScore(exam.getId(), exam.getExamDate(), exam.getCoefficient(), score);
  }

  private BigDecimal averageExams(List<ExamScore> exams) {
    BigDecimal weighted = BigDecimal.ZERO;
    BigDecimal totalCoefficient = BigDecimal.ZERO;
    for (ExamScore exam : exams) {
      weighted = weighted.add(exam.score().multiply(exam.coefficient()));
      totalCoefficient = totalCoefficient.add(exam.coefficient());
    }
    if (totalCoefficient.compareTo(BigDecimal.ZERO) == 0) {
      return null;
    }
    return weighted;
  }

  private BigDecimal average(List<CourseReportEntry> courses) {
    BigDecimal weighted = BigDecimal.ZERO;
    long totalCredits = 0;
    for (CourseReportEntry course : courses) {
      if (course.average() == null) {
        continue;
      }
      weighted = weighted.add(course.average().multiply(BigDecimal.valueOf(course.credits())));
      totalCredits += course.credits();
    }
    if (totalCredits == 0) {
      return null;
    }
    return weighted.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);
  }

  private JUser resolveStudent(String studentId) {
    JUser student =
        userRepository
            .findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
    if (student.getRole() != Role.STUDENT) {
      throw new BadRequestException("Only students can have a report");
    }
    return student;
  }

  private void requireCanViewReport(String studentId) {
    JUser current = SecurityUtils.currentUser();
    if (current.getRole() == Role.ADMIN) {
      return;
    }
    if (current.getRole() == Role.STUDENT && current.getId().equals(studentId)) {
      return;
    }
    throw new ForbiddenException("You are not allowed to view this report");
  }

  private record CourseParticipation(
      JStudentGroup group, JPromotion promotion, JGroupCourse groupCourse) {}

  private record CourseKey(UUID promotionId, UUID courseId) {}
}
