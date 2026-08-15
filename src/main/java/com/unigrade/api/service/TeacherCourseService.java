package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.TeacherCourseMapper;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.TeacherCourse;
import com.unigrade.api.model.dto.TeacherAssignmentRequest;
import com.unigrade.api.model.dto.TeacherPriorityRequest;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.TeacherCourseRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JTeacherCourse;
import com.unigrade.api.repository.model.JUser;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherCourseService {

  private final TeacherCourseRepository repository;
  private final CourseRepository courseRepository;
  private final UserRepository userRepository;
  private final TeacherCourseMapper mapper;

  public List<TeacherCourse> findByCourse(UUID courseId) {
    if (!courseRepository.existsById(courseId)) {
      throw new NotFoundException("Course not found: " + courseId);
    }
    return repository.findByCourseIdOrderByPriorityAscTeacherIdAsc(courseId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Transactional
  public TeacherCourse assign(UUID courseId, TeacherAssignmentRequest request) {
    JCourse course = resolveCourse(courseId);
    JUser teacher = resolveTeacher(request.teacherId());

    if (repository.existsByCourseIdAndTeacherId(courseId, teacher.getId())) {
      throw new ConflictException("Teacher is already assigned to this course");
    }

    var teacherCourse = new JTeacherCourse(null, course, teacher, request.priority());
    return mapper.toDomain(raceAwareSave(teacherCourse));
  }

  @Transactional
  public TeacherCourse updatePriority(
      UUID courseId, String teacherId, TeacherPriorityRequest request) {
    JTeacherCourse teacherCourse = resolveAssignment(courseId, teacherId);
    teacherCourse.setPriority(request.priority());
    return mapper.toDomain(repository.save(teacherCourse));
  }

  @Transactional
  public void remove(UUID courseId, String teacherId) {
    repository.delete(resolveAssignment(courseId, teacherId));
  }

  private JTeacherCourse resolveAssignment(UUID courseId, String teacherId) {
    if (!courseRepository.existsById(courseId)) {
      throw new NotFoundException("Course not found: " + courseId);
    }
    return repository
        .findByCourseIdAndTeacherId(courseId, teacherId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Teacher " + teacherId + " is not assigned to course " + courseId));
  }

  private JCourse resolveCourse(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
  }

  private JUser resolveTeacher(String teacherId) {
    JUser teacher =
        userRepository
            .findById(teacherId)
            .orElseThrow(() -> new NotFoundException("Teacher not found: " + teacherId));
    if (teacher.getRole() != Role.TEACHER) {
      throw new BadRequestException("User is not a teacher");
    }
    if (!teacher.getIsActive()) {
      throw new BadRequestException("Teacher is inactive");
    }
    return teacher;
  }

  private JTeacherCourse raceAwareSave(JTeacherCourse teacherCourse) {
    try {
      return repository.save(teacherCourse);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Teacher is already assigned to this course");
    }
  }
}
