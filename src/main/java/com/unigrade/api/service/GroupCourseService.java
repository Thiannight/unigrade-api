package com.unigrade.api.service;

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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupCourseService {

  private static final short MAX_CREDITS_PER_SEMESTER = 30;

  private final GroupCourseRepository repository;
  private final StudentGroupRepository groupRepository;
  private final CourseRepository courseRepository;
  private final ExamRepository examRepository;
  private final GroupCourseMapper mapper;

  public List<GroupCourse> findActiveByGroup(UUID groupId) {
    if (!groupRepository.existsById(groupId)) {
      throw new NotFoundException("Group not found: " + groupId);
    }
    return repository.findAllByGroupIdAndEndDateIsNull(groupId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public GroupCourse assign(UUID groupId, GroupCourseAssignRequest request) {
    JStudentGroup group = resolveGroup(groupId);
    JCourse course = resolveCourse(request.courseId());
    checkAlreadyAssigned(groupId, request.courseId());
    checkCreditTotal(groupId, request.semester(), course.getCredits());

    var groupCourse =
        new GroupCourse(
            null, groupId, request.courseId(), request.semester(), request.startDate(), null);
    return mapper.toDomain(raceAwareSave(mapper.toEntity(groupCourse, course, group)));
  }

  public GroupCourse end(UUID groupId, UUID courseId, GroupCourseEndRequest request) {
    JGroupCourse active =
        repository
            .findByGroupIdAndCourseIdAndEndDateIsNull(groupId, courseId)
            .orElseThrow(() -> noActiveAssignment(groupId, courseId));

    if (request.endDate().isBefore(active.getStartDate())) {
      throw new BadRequestException("endDate must not be before startDate");
    }

    active.setEndDate(request.endDate());
    return mapper.toDomain(repository.saveAndFlush(active));
  }

  public void delete(UUID groupId, UUID courseId) {
    JGroupCourse active =
        repository
            .findByGroupIdAndCourseIdAndEndDateIsNull(groupId, courseId)
            .orElseThrow(() -> noActiveAssignment(groupId, courseId));

    if (examRepository.existsByGroupCourseId(active.getId())) {
      throw new ConflictException("Cannot delete: exams exist for this course assignment");
    }

    repository.delete(active);
  }

  private void checkAlreadyAssigned(UUID groupId, UUID courseId) {
    if (repository.findByGroupIdAndCourseId(groupId, courseId).isPresent()) {
      throw new ConflictException("Course is already assigned to this group");
    }
  }

  private void checkCreditTotal(UUID groupId, Semester semester, Short credits) {
    long total = repository.sumCreditsByGroupIdAndSemester(groupId, semester) + credits;
    if (total > MAX_CREDITS_PER_SEMESTER) {
      throw new BadRequestException(
          "Semester credit limit exceeded (max " + MAX_CREDITS_PER_SEMESTER + ")");
    }
  }

  private JGroupCourse raceAwareSave(JGroupCourse groupCourse) {
    try {
      return repository.save(groupCourse);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Course is already assigned to this group");
    }
  }

  private JStudentGroup resolveGroup(UUID groupId) {
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
  }

  private JCourse resolveCourse(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
  }

  private NotFoundException noActiveAssignment(UUID groupId, UUID courseId) {
    return new NotFoundException(
        "No active course assignment for course " + courseId + " in group " + groupId);
  }
}
