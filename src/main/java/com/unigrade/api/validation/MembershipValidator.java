package com.unigrade.api.validation;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.Semester;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.GradeCalculationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipValidator {

  private final GradeCalculationService gradeCalculationService;
  private final GroupCourseRepository groupCourseRepository;

  public void validateTransfer(
      JUser student, JMembership oldMembership, GroupTransferRequest request) {
    if (!student.getIsActive()) {
      throw new BadRequestException("Student is inactive");
    }
    if (student.getRole() != Role.STUDENT) {
      throw new BadRequestException("Only students can be members of a group");
    }
    if (oldMembership == null) {
      return;
    }
    if (oldMembership.getGroup().getId().equals(request.newGroupId())) {
      throw new BadRequestException("Cannot transfer to same group");
    }
    if (request.transferDate().isBefore(oldMembership.getStartDate())) {
      throw new BadRequestException("transferDate must not be before current membership startDate");
    }
    checkSemesterComplete(oldMembership.getGroup().getId(), student.getId(), oldMembership);
  }

  private void checkSemesterComplete(UUID groupId, String studentId, JMembership membership) {
    List<JGroupCourse> courses = groupCourseRepository.findAllByGroupId(groupId);
    List<JMembership> single = List.of(membership);

    Map<Semester, List<JGroupCourse>> bySemester =
        courses.stream().collect(Collectors.groupingBy(JGroupCourse::getSemester));

    boolean transferable =
        bySemester.values().stream()
            .anyMatch(
                semesterCourses -> {
                  long credits =
                      semesterCourses.stream().mapToLong(gc -> gc.getCourse().getCredits()).sum();
                  boolean allComplete =
                      semesterCourses.stream()
                          .allMatch(
                              gc ->
                                  gradeCalculationService
                                      .computeCourseResult(gc.getId(), studentId, single)
                                      .completed());
                  return credits >= Level.PER_LEVEL_CREDIT / 2 && allComplete;
                });

    if (!transferable) {
      throw new BadRequestException(
          "Cannot transfer: no completed semester with "
              + (Level.PER_LEVEL_CREDIT / 2)
              + " credits");
    }
  }
}
