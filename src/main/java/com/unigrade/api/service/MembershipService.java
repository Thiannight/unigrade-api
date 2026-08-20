package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.MembershipMapper;
import com.unigrade.api.model.Membership;
import com.unigrade.api.model.dto.GroupTransferRequest;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.StudentGroupRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.validation.MembershipValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipService {

  private final MembershipRepository repository;
  private final StudentGroupRepository groupRepository;
  private final UserRepository userRepository;
  private final MembershipMapper mapper;
  private final MembershipValidator validator;

  public List<Membership> getStudentMemberships(String studentId) {
    return repository.findByStudentId(studentId).stream().map(mapper::toDomain).toList();
  }

  @Transactional
  public Membership transfer(String studentId, GroupTransferRequest request) {
    LocalDate transferDate = request.transferDate();
    UUID newGroupId = request.newGroupId();

    JUser student = resolveStudent(studentId);
    JStudentGroup group = resolveGroup(newGroupId);
    JMembership oldMembership = repository.findByStudentIdAndEndDateIsNull(studentId).orElse(null);

    validator.validateTransfer(student, oldMembership, request);

    if (oldMembership != null) {
      oldMembership.setEndDate(transferDate);
      repository.saveAndFlush(oldMembership);
    }

    var membership = new Membership(null, newGroupId, studentId, transferDate, null);
    return mapper.toDomain(raceAwareSave(mapper.toEntity(membership, group, student)));
  }

  public List<Membership> getMembersAt(
      UUID groupId, LocalDate date, boolean includeInactive, int page, int size) {
    if (!groupRepository.existsById(groupId)) {
      throw new NotFoundException("Group not found: " + groupId);
    }

    return repository
        .findMembersAt(
            groupId,
            date,
            includeInactive,
            PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 20)))
        .map(mapper::toDomain)
        .toList();
  }

  private JMembership raceAwareSave(JMembership membership) {
    try {
      return repository.save(membership);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Student is already a member of a group");
    }
  }

  private JStudentGroup resolveGroup(UUID groupId) {
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
  }

  private JUser resolveStudent(String studentId) {
    return userRepository
        .findById(studentId)
        .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
  }
}
