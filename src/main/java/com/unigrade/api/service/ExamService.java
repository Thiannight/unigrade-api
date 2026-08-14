package com.unigrade.api.service;

import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamService {

  private final ExamRepository repository;
  private final CourseRepository courseRepository;
  private final ExamMapper mapper;

  public List<Exam> findAll(UUID courseId, int page, int size) {
    findCourse(courseId);
    return repository
        .findAllByCourseId(courseId, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)))
        .map(mapper::toDomain)
        .toList();
  }

  public Exam findById(UUID courseId, UUID id) {
    return repository
        .findByIdAndCourseId(id, courseId)
        .map(mapper::toDomain)
        .orElseThrow(() -> notFound(id));
  }

  public Exam create(UUID courseId, Exam exam) {
    JCourse course = findCourse(courseId);
    return mapper.toDomain(repository.save(mapper.toEntity(exam, course)));
  }

  public Exam update(UUID courseId, UUID id, Exam exam) {
    JCourse course = findCourse(courseId);
    if (!repository.existsByIdAndCourseId(id, courseId)) {
      throw notFound(id);
    }
    var withId = new Exam(id, exam.examDate(), exam.coefficient(), courseId);
    JExam saved = repository.save(mapper.toEntity(withId, course));
    return mapper.toDomain(saved);
  }

  public void delete(UUID courseId, UUID id) {
    if (!repository.existsByIdAndCourseId(id, courseId)) {
      throw notFound(id);
    }
    repository.deleteById(id);
  }

  private JCourse findCourse(UUID courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
  }

  private static NotFoundException notFound(UUID id) {
    return new NotFoundException("Exam not found: " + id);
  }
}
