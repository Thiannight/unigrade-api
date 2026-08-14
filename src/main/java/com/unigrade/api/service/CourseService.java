package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.CourseMapper;
import com.unigrade.api.model.Course;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.model.JCourse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

  private final CourseRepository repository;
  private final CourseMapper mapper;

  public List<Course> findAll() {
    return repository.findAll().stream().map(mapper::toDomain).toList();
  }

  public Course findById(UUID id) throws NotFoundException {
    return repository.findById(id).map(mapper::toDomain).orElseThrow(() -> notFound(id));
  }

  public Course create(Course course) throws ConflictException {
    return saveAndMap(mapper.toEntity(course));
  }

  public Course update(UUID id, Course course) throws NotFoundException, ConflictException {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    var withId = new Course(id, course.reference(), course.title(), course.credits());
    return saveAndMap(mapper.toEntity(withId));
  }

  public void delete(UUID id) throws NotFoundException {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    repository.deleteById(id);
  }

  private Course saveAndMap(JCourse entity) throws ConflictException {
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
