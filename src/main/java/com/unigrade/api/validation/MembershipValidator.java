package com.unigrade.api.validation;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class MembershipValidator {

  public void validateStudent(JUser student) {
    if (student.getRole() != Role.STUDENT) {
      throw new BadRequestException("Only students can be members of a group");
    }
    if (!student.getIsActive()) {
      throw new BadRequestException("Student is inactive");
    }
    var currentGroup = student.getCurrentGroup();
    if (currentGroup != null) {
      throw new BadRequestException("Student is already in a group");
    }
  }

  public void validateTransfer(
      JUser student, JMembership oldMembership, GroupTransferRequest request) {
    if (oldMembership.getGroup().getId().equals(request.newGroupId())) {
      throw new BadRequestException("Cannot transfer to same group");
    }
    if (request.transferDate().isBefore(oldMembership.getStartDate())) {
      throw new BadRequestException("transferDate must not be before current membership startDate");
    }
    if (student.getRole() != Role.STUDENT) {
      throw new BadRequestException("Only students can be members of a group");
    }
    if (!student.getIsActive()) {
      throw new BadRequestException("Student is inactive");
    }
  }
}
