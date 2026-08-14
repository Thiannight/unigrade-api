package com.unigrade.api.validation;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.model.Role;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
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

  public void validateTransferDate(LocalDate transferDate, LocalDate membershipStartDate) {
    if (transferDate.isBefore(membershipStartDate)) {
      throw new BadRequestException("transferDate must not be before startDate");
    }
  }
}
