package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.GroupCourseMapper;
import com.unigrade.api.model.GroupCourse;
import com.unigrade.api.model.Semester;
import com.unigrade.api.model.dto.GroupCourseAssignRequest;
import com.unigrade.api.model.dto.GroupCourseEndRequest;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.StudentGroupRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JStudentGroup;
import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class GroupCourseServiceTest {

  private static final UUID GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID GROUP_COURSE_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate END_DATE = LocalDate.of(2024, 6, 1);
  private static final Semester SEMESTER = Semester.S3;

  @Mock private GroupCourseRepository repository;
  @Mock private StudentGroupRepository groupRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private ExamRepository examRepository;
  private final GroupCourseMapper mapper = new GroupCourseMapper();
  private GroupCourseService service;

  @BeforeEach
  void setUp() {
    service =
        new GroupCourseService(
            repository, groupRepository, courseRepository, examRepository, mapper);
  }

  @Test
  void findActiveByGroup_returnsMappedList() {
    when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
    when(repository.findAllByGroupIdAndEndDateIsNull(GROUP_ID)).thenReturn(List.of(groupCourse()));

    List<GroupCourse> result = service.findUnfinishedByGroupId(GROUP_ID);

    assertEquals(1, result.size());
    assertEquals(COURSE_ID, result.get(0).courseId());
  }

  @Test
  void findActiveByGroup_missingGroup_throwsNotFound() {
    when(groupRepository.existsById(GROUP_ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.findUnfinishedByGroupId(GROUP_ID));
  }

  @Test
  void findByGroupId_returnsMappedList() {
    when(groupRepository.existsById(GROUP_ID)).thenReturn(true);
    when(repository.findAllByGroupId(GROUP_ID)).thenReturn(List.of(groupCourse()));

    List<GroupCourse> result = service.findByGroupId(GROUP_ID);

    assertEquals(1, result.size());
    assertEquals(COURSE_ID, result.get(0).courseId());
  }

  @Test
  void findByGroupId_missingGroup_throwsNotFound() {
    when(groupRepository.existsById(GROUP_ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.findByGroupId(GROUP_ID));
  }

  @Test
  void assign_saves() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.sumCreditsByGroupIdAndSemester(GROUP_ID, SEMESTER)).thenReturn(0L);
    when(repository.save(any())).thenReturn(groupCourse());

    GroupCourse result = service.assign(GROUP_ID, request);

    assertEquals(COURSE_ID, result.courseId());
    verify(repository).save(any());
  }

  @Test
  void assign_atSemesterCreditCap_saves() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.sumCreditsByGroupIdAndSemester(GROUP_ID, SEMESTER)).thenReturn(24L);
    when(repository.save(any())).thenReturn(groupCourse());

    GroupCourse result = service.assign(GROUP_ID, request);

    assertEquals(COURSE_ID, result.courseId());
    verify(repository).save(any());
  }

  @Test
  void assign_exceedsSemesterCreditCap_throwsBadRequest() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.sumCreditsByGroupIdAndSemester(GROUP_ID, SEMESTER)).thenReturn(25L);

    BadRequestException exception =
        assertThrows(BadRequestException.class, () -> service.assign(GROUP_ID, request));

    assertTrue(exception.getMessage().contains("Semester credit limit exceeded"));
  }

  @Test
  void assign_missingGroup_throwsNotFound() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.assign(GROUP_ID, request));

    assertTrue(exception.getMessage().contains("Group not found"));
  }

  @Test
  void assign_missingCourse_throwsNotFound() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.assign(GROUP_ID, request));

    assertTrue(exception.getMessage().contains("Course not found"));
  }

  @Test
  void assign_duplicateActiveConstraintRace_throwsConflict() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.sumCreditsByGroupIdAndSemester(GROUP_ID, SEMESTER)).thenReturn(0L);
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(ConflictException.class, () -> service.assign(GROUP_ID, request));
  }

  @Test
  void assign_alreadyAssigned_throwsConflict() {
    var request = new GroupCourseAssignRequest(COURSE_ID, SEMESTER, START_DATE);
    when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group()));
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.findByGroupIdAndCourseId(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(groupCourse()));

    assertThrows(ConflictException.class, () -> service.assign(GROUP_ID, request));
  }

  @Test
  void end_closesActiveAssignment() {
    JGroupCourse active = groupCourse();
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(active));
    when(examRepository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID)).thenReturn(BigDecimal.ONE);
    when(repository.saveAndFlush(active)).thenReturn(active);

    GroupCourse result = service.end(GROUP_ID, COURSE_ID, new GroupCourseEndRequest(END_DATE));

    assertEquals(END_DATE, active.getEndDate());
    assertEquals(END_DATE, result.endDate());
    verify(repository).saveAndFlush(active);
  }

  @Test
  void end_insufficientExamCoefficient_throwsBadRequest() {
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(groupCourse()));
    when(examRepository.sumCoefficientByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(new BigDecimal("0.5"));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> service.end(GROUP_ID, COURSE_ID, new GroupCourseEndRequest(END_DATE)));

    assertTrue(exception.getMessage().contains("must have 1"));
  }

  @Test
  void end_noActiveAssignment_throwsNotFound() {
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> service.end(GROUP_ID, COURSE_ID, new GroupCourseEndRequest(END_DATE)));

    assertTrue(exception.getMessage().contains("No active course assignment"));
  }

  @Test
  void end_dateBeforeStart_throwsBadRequest() {
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(groupCourse()));

    assertThrows(
        BadRequestException.class,
        () -> service.end(GROUP_ID, COURSE_ID, new GroupCourseEndRequest(START_DATE.minusDays(1))));
  }

  @Test
  void delete_noExams_deletesActiveAssignment() {
    JGroupCourse active = groupCourse();
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(active));
    when(examRepository.existsByGroupCourseId(GROUP_COURSE_ID)).thenReturn(false);

    service.delete(GROUP_ID, COURSE_ID);

    verify(repository).delete(active);
  }

  @Test
  void delete_examsExist_throwsConflict() {
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.of(groupCourse()));
    when(examRepository.existsByGroupCourseId(GROUP_COURSE_ID)).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.delete(GROUP_ID, COURSE_ID));
  }

  @Test
  void delete_noActiveAssignment_throwsNotFound() {
    when(repository.findByGroupIdAndCourseIdAndEndDateIsNull(GROUP_ID, COURSE_ID))
        .thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.delete(GROUP_ID, COURSE_ID));

    assertTrue(exception.getMessage().contains("No active course assignment"));
  }

  private JStudentGroup group() {
    var group = new JStudentGroup();
    group.setId(GROUP_ID);
    return group;
  }

  private JCourse course() {
    var course = new JCourse();
    course.setId(COURSE_ID);
    course.setCredits((short) 6);
    return course;
  }

  private JGroupCourse groupCourse() {
    return JGroupCourse.builder()
        .id(GROUP_COURSE_ID)
        .group(group())
        .course(course())
        .semester(SEMESTER)
        .startDate(START_DATE)
        .endDate(null)
        .build();
  }
}
