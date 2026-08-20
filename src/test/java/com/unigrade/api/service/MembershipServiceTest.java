package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.MembershipMapper;
import com.unigrade.api.model.Membership;
import com.unigrade.api.model.Role;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

  private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID NEW_GROUP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID MEMBERSHIP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String STUDENT_ID = "STD26001";
  private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2024, 6, 1);

  @Mock private MembershipRepository repository;
  @Mock private StudentGroupRepository groupRepository;
  @Mock private UserRepository userRepository;
  @Mock private MembershipValidator validator;
  private final MembershipMapper mapper = new MembershipMapper();
  private MembershipService service;

  @BeforeEach
  void setUp() {
    service = new MembershipService(repository, groupRepository, userRepository, mapper, validator);
  }

  @Test
  void transfer_movesStudent() {
    JMembership membership = membership();
    JUser student = student();
    when(repository.findByStudentIdAndEndDateIsNull(STUDENT_ID))
        .thenReturn(Optional.of(membership));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(groupRepository.findById(NEW_GROUP_ID)).thenReturn(Optional.of(newGroup()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

    Membership result =
        service.transfer(STUDENT_ID, new GroupTransferRequest(NEW_GROUP_ID, END_DATE));

    assertEquals(END_DATE, membership.getEndDate());
    assertEquals(STUDENT_ID, result.studentId());
    assertEquals(NEW_GROUP_ID, result.groupId());
    verify(repository).saveAndFlush(membership);
  }

  @Test
  void transfer_missingUser_throwsNotFound() {
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> service.transfer(STUDENT_ID, new GroupTransferRequest(NEW_GROUP_ID, END_DATE)));

    assertTrue(exception.getMessage().contains("Student not found"));
  }

  @Test
  void transfer_missingGroup_throwsNotFound() {
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(groupRepository.findById(NEW_GROUP_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> service.transfer(STUDENT_ID, new GroupTransferRequest(NEW_GROUP_ID, END_DATE)));

    assertTrue(exception.getMessage().contains("Group not found"));
  }

  @Test
  void transfer_noActiveMembership_createsMembership() {
    when(repository.findByStudentIdAndEndDateIsNull(STUDENT_ID)).thenReturn(Optional.empty());
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    when(groupRepository.findById(NEW_GROUP_ID)).thenReturn(Optional.of(newGroup()));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Membership result =
        service.transfer(STUDENT_ID, new GroupTransferRequest(NEW_GROUP_ID, END_DATE));

    assertEquals(STUDENT_ID, result.studentId());
    assertEquals(NEW_GROUP_ID, result.groupId());
    assertNull(result.endDate());
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void transfer_assignRace_throwsConflict() {
    JMembership membership = membership();
    JUser student = student();
    when(repository.findByStudentIdAndEndDateIsNull(STUDENT_ID))
        .thenReturn(Optional.of(membership));
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
    when(groupRepository.findById(NEW_GROUP_ID)).thenReturn(Optional.of(newGroup()));
    when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

    assertThrows(
        ConflictException.class,
        () -> service.transfer(STUDENT_ID, new GroupTransferRequest(NEW_GROUP_ID, END_DATE)));
  }

  @Test
  void getMembersAt_returnsMappedList() {
    when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
    when(repository.findMembersAt(any(), any(), anyBoolean(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(membership())));

    List<Membership> result = service.getMembersAt(GROUP_ID, END_DATE, false, 1, 10);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.get(0).studentId());
  }

  @Test
  void getMembersAt_missingGroup_throwsNotFound() {
    assertThrows(
        NotFoundException.class, () -> service.getMembersAt(GROUP_ID, END_DATE, false, 0, 20));
  }

  @Test
  void getStudentMemberships_returnsMappedList() {
    when(repository.findByStudentId(STUDENT_ID)).thenReturn(List.of(membership()));

    List<Membership> result = service.getStudentMemberships(STUDENT_ID);

    assertEquals(1, result.size());
    assertEquals(STUDENT_ID, result.get(0).studentId());
  }

  private JStudentGroup group() {
    var group = new JStudentGroup();
    group.setId(GROUP_ID);
    return group;
  }

  private JStudentGroup newGroup() {
    var group = new JStudentGroup();
    group.setId(NEW_GROUP_ID);
    return group;
  }

  private JUser student() {
    var student = new JUser();
    student.setId(STUDENT_ID);
    student.setRole(Role.STUDENT);
    student.setIsActive(true);
    student.setMemberships(List.of());
    return student;
  }

  private JMembership membership() {
    return JMembership.builder()
        .id(MEMBERSHIP_ID)
        .group(group())
        .student(student())
        .startDate(START_DATE)
        .endDate(null)
        .build();
  }
}
