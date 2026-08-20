package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.CourseMapper;
import com.unigrade.api.model.Course;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JCourse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

  private final CourseRepository repository;
  private final GroupCourseRepository groupCourseRepository;
  private final CourseMapper mapper;

  public List<Course> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)))
        .map(mapper::toDomain)
        .toList();
  }

  public Course findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain).orElseThrow(() -> notFound(id));
  }

  public Course create(Course course) {
    return saveAndMap(mapper.toEntity(course));
  }

  public Course update(UUID id, Course course) {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    var withId = new Course(id, course.reference(), course.title(), course.credits());
    return saveAndMap(mapper.toEntity(withId));
  }

  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    if (groupCourseRepository.existsByCourseId(id)) {
      throw new ConflictException("Cannot delete: course is assigned to groups");
    }
    repository.deleteById(id);
  }

  private Course saveAndMap(JCourse entity) {
    try {
      return mapper.toDomain(repository.save(entity));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Course reference or title already exists");
    }
  }

  private static NotFoundException notFound(UUID id) {
    return new NotFoundException("Course not found: " + id);
  }
}
