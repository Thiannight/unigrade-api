package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.CourseMapper;
import com.unigrade.api.model.Course;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.model.JCourse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

  private final CourseRepository repository;
  private final CourseMapper mapper;

  public Page<Course> findAll(Pageable pageable) {
    return repository.findAll(pageable).map(mapper::toDomain);
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
