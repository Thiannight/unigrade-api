package com.unigrade.api.service;

import com.unigrade.api.mapper.CourseMapper;
import com.unigrade.api.model.Course;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.model.JCourse;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseMapper courseMapper;

  public List<Course> findAll() {
    return courseRepository.findAll().stream().map(courseMapper::toDomain).toList();
  }

  public Course findById(UUID id) {
    return courseRepository
        .findById(id)
        .map(courseMapper::toDomain)
        .orElseThrow(() -> notFound(id));
  }

  public Course create(Course course) {
    var withoutId = new Course(null, course.reference(), course.title(), course.credits());
    return saveAndMap(courseMapper.toEntity(withoutId));
  }

  public Course update(UUID id, Course course) {
    if (!courseRepository.existsById(id)) {
      throw notFound(id);
    }
    var withId = new Course(id, course.reference(), course.title(), course.credits());
    return saveAndMap(courseMapper.toEntity(withId));
  }

  public void delete(UUID id) {
    if (!courseRepository.existsById(id)) {
      throw notFound(id);
    }
    courseRepository.deleteById(id);
  }

  private Course saveAndMap(JCourse entity) {
    try {
      return courseMapper.toDomain(courseRepository.save(entity));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Course reference or title already exists", e);
    }
  }

  private static ResponseStatusException notFound(UUID id) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + id);
  }
}
