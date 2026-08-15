package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.GroupCourseMapper;
import com.unigrade.api.model.GroupCourse;
import com.unigrade.api.model.dto.GroupCourseAssignRequest;
import com.unigrade.api.model.dto.GroupCourseEndRequest;
import com.unigrade.api.repository.CourseRepository;
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

  private final GroupCourseRepository repository;
  private final StudentGroupRepository groupRepository;
  private final CourseRepository courseRepository;
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

    var groupCourse = new GroupCourse(null, groupId, request.courseId(), request.startDate(), null);
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
