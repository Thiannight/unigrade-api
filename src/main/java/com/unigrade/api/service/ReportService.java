package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.model.CourseReportEntry;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.LevelReport;
import com.unigrade.api.model.ReportStatus;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.StudentReport;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.SecurityUtils;
import com.unigrade.api.service.GradeCalculationService.CourseData;
import com.unigrade.api.service.GradeCalculationService.CourseKey;
import com.unigrade.api.service.GradeCalculationService.CourseParticipation;
import com.unigrade.api.service.GradeCalculationService.CourseResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final UserRepository userRepository;
  private final GradeCalculationService gradeCalculationService;

  @Transactional(readOnly = true)
  public StudentReport generate(String studentId, Level levelFilter) {
    requireCanViewReport(studentId);
    return generateReport(studentId, levelFilter);
  }

  @Transactional(readOnly = true)
  public StudentReport generateForSystem(String studentId, Level levelFilter) {
    return generateReport(studentId, levelFilter);
  }

  private StudentReport generateReport(String studentId, Level levelFilter) {
    JUser student = resolveStudent(studentId);

    CourseData courseData = gradeCalculationService.resolveAllCoursesByLevels(studentId);

    Map<Level, Map<CourseKey, CourseParticipation>> coursesByLevel = courseData.coursesByLevel();

    List<LevelReport> levelReports = new ArrayList<>();

    for (Level level : Level.values()) {
      if (levelFilter != null && level != levelFilter) {
        continue;
      }

      Map<CourseKey, CourseParticipation> courses = coursesByLevel.get(level);

      if (courses == null || courses.isEmpty()) {
        continue;
      }

      levelReports.add(
          buildLevelReport(
              level,
              courses,
              studentId,
              courseData.memberships()));
    }

    List<CourseReportEntry> allCourses = levelReports.stream()
        .flatMap(levelReport -> levelReport.courses().stream())
        .toList();

    long totalCredits = levelReports.stream()
        .mapToLong(LevelReport::totalCredits)
        .sum();

    int expectedLevels = (levelFilter != null) ? 1 : Level.values().length;

    long requiredCredits = (long) expectedLevels * Level.PER_LEVEL_CREDIT;

    ReportStatus status;

    if (levelReports.size() < expectedLevels) {
      status = ReportStatus.TEMPORARY;
    } else {
      status = levelReports.stream()
          .anyMatch(
              levelReport -> levelReport.status() == ReportStatus.TEMPORARY)
                  ? ReportStatus.TEMPORARY
                  : ReportStatus.COMPLETE;
    }

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

  private LevelReport buildLevelReport(
      Level level,
      Map<CourseKey, CourseParticipation> coursesByCourse,
      String studentId,
      List<JMembership> memberships) {

    List<CourseReportEntry> courses = new ArrayList<>();

    for (Map.Entry<CourseKey, CourseParticipation> entry : coursesByCourse.entrySet()) {

      CourseParticipation participation = entry.getValue();
      JGroupCourse representative = participation.groupCourse();

      String promotionReference = participation.promotion().getReference();

      CourseResult result = gradeCalculationService.computeCourseResult(
          representative.getId(),
          studentId,
          memberships);

      courses.add(
          new CourseReportEntry(
              representative.getCourse().getId(),
              promotionReference,
              representative.getCourse().getReference(),
              representative.getCourse().getTitle(),
              representative.getCourse().getCredits(),
              result.completed(),
              result.average(),
              result.exams()));
    }

    boolean allCompleted = courses.stream().allMatch(CourseReportEntry::completed);

    long totalCredits = courses.stream()
        .mapToLong(CourseReportEntry::credits)
        .sum();

    ReportStatus status = allCompleted && totalCredits >= Level.PER_LEVEL_CREDIT
        ? ReportStatus.COMPLETE
        : ReportStatus.TEMPORARY;

    return new LevelReport(
        level,
        status,
        totalCredits,
        Level.PER_LEVEL_CREDIT,
        average(courses),
        courses);
  }

  private BigDecimal average(List<CourseReportEntry> courses) {
    BigDecimal weighted = BigDecimal.ZERO;
    long totalCredits = 0;

    for (CourseReportEntry course : courses) {
      if (course.average() == null) {
        continue;
      }

      weighted = weighted.add(
          course.average()
              .multiply(BigDecimal.valueOf(course.credits())));

      totalCredits += course.credits();
    }

    if (totalCredits == 0) {
      return null;
    }

    return weighted.divide(
        BigDecimal.valueOf(totalCredits),
        2,
        RoundingMode.HALF_UP);
  }

  private JUser resolveStudent(String studentId) {
    JUser student = userRepository
        .findById(studentId)
        .orElseThrow(
            () -> new NotFoundException(
                "Student not found: " + studentId));

    if (student.getRole() != Role.STUDENT) {
      throw new BadRequestException(
          "Only students can have a report");
    }

    return student;
  }

  private void requireCanViewReport(String studentId) {
    JUser current = SecurityUtils.currentUser();

    if (current.getRole() == Role.ADMIN) {
      return;
    }

    if (current.getRole() == Role.STUDENT
        && current.getId().equals(studentId)) {
      return;
    }

    throw new ForbiddenException(
        "You are not allowed to view this report");
  }
}
