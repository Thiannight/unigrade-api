package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamService {

  private static final BigDecimal MAX_TOTAL_COEFFICIENT = new BigDecimal("100");

  private final ExamRepository repository;
  private final CourseRepository courseRepository;
  private final ExamMapper mapper;

  public List<Exam> findAll(UUID courseId, Short schoolYear, Short semester, int page, int size) {
    findCourse(courseId);
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50));

    Page<JExam> result;
    if (schoolYear == null) {
      result = repository.findAllByCourseId(courseId, pageable);
    } else if (semester == null) {
      result = repository.findAllByCourseIdAndSchoolYear(courseId, schoolYear, pageable);
    } else {
      result =
          repository.findAllByCourseIdAndSchoolYearAndSemester(
              courseId, schoolYear, semester, pageable);
    }
    return result.map(mapper::toDomain).toList();
  }

  public Exam findById(UUID courseId, UUID id) {
    return repository
        .findByIdAndCourseId(id, courseId)
        .map(mapper::toDomain)
        .orElseThrow(() -> notFound(id));
  }

  public Exam create(UUID courseId, Exam exam) {
    JCourse course = findCourse(courseId);
    validateTotalCoefficient(
        courseId, exam.schoolYear(), exam.semester(), exam.coefficient(), null);
    return mapper.toDomain(repository.save(mapper.toEntity(exam, course)));
  }

  public Exam update(UUID courseId, UUID id, Exam exam) {
    JCourse course = findCourse(courseId);
    if (!repository.existsByIdAndCourseId(id, courseId)) {
      throw notFound(id);
    }
    validateTotalCoefficient(courseId, exam.schoolYear(), exam.semester(), exam.coefficient(), id);
    var withId =
        new Exam(
            id, exam.examDate(), exam.coefficient(), courseId, exam.schoolYear(), exam.semester());
    JExam saved = repository.save(mapper.toEntity(withId, course));
    return mapper.toDomain(saved);
  }

  public void delete(UUID courseId, UUID id) {
    if (!repository.existsByIdAndCourseId(id, courseId)) {
      throw notFound(id);
    }
    repository.deleteById(id);
  }

  private void validateTotalCoefficient(
      UUID courseId,
      Short schoolYear,
      Short semester,
      BigDecimal newCoefficient,
      UUID excludeExamId) {
    BigDecimal existingTotal =
        repository.findByCourseIdAndSchoolYearAndSemester(courseId, schoolYear, semester).stream()
            .filter(e -> excludeExamId == null || !e.getId().equals(excludeExamId))
            .map(JExam::getCoefficient)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal total = existingTotal.add(newCoefficient);
    if (total.compareTo(MAX_TOTAL_COEFFICIENT) > 0) {
      throw new BadRequestException(
          "Total exam coefficient for this course in school year "
              + schoolYear
              + " semester "
              + semester
              + " would be "
              + total
              + "%, cannot exceed 100%");
    }
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
