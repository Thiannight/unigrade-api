package com.unigrade.api.validation;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Level;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.service.GradeCalculationService;
import java.util.List;
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
    checkSufficientCreditTotal(oldMembership.getGroup().getId());
    checkAllCoursesComplete(oldMembership.getGroup().getId(), student.getId(), oldMembership);
  }

  private void checkSufficientCreditTotal(java.util.UUID groupId) {
    List<JGroupCourse> courses = groupCourseRepository.findAllByGroupId(groupId);
    long total = courses.stream().mapToLong(gc -> gc.getCourse().getCredits()).sum();
    if (total != Level.PER_LEVEL_CREDIT / 2) {
      throw new BadRequestException(
          "Cannot transfer yet: Minimum level credit of "
              + (Level.PER_LEVEL_CREDIT / 2)
              + " for semester not reached");
    }
  }

  private void checkAllCoursesComplete(
      java.util.UUID groupId, String studentId, JMembership membership) {
    List<JGroupCourse> courses = groupCourseRepository.findAllByGroupId(groupId);
    List<JMembership> single = List.of(membership);
    boolean allComplete =
        courses.stream()
            .allMatch(
                gc ->
                    gradeCalculationService
                        .computeCourseResult(gc.getId(), studentId, single)
                        .completed());
    if (!allComplete) {
      throw new BadRequestException(
          "All courses in current group must be completed before transfer");
    }
  }
}
