package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExamService {

  private static final BigDecimal MAX_TOTAL_COEFFICIENT = BigDecimal.ONE;

  private final ExamRepository repository;
  private final GroupCourseRepository groupCourseRepository;
  private final ExamMapper mapper;

  public List<Exam> findAll(UUID groupCourseId, int page, int size) {
    findGroupCourse(groupCourseId);
    return repository
        .findAllByGroupCourseId(
            groupCourseId, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)))
        .map(mapper::toDomain)
        .toList();
  }

  public Exam findById(UUID groupCourseId, UUID id) {
    return repository
        .findByIdAndGroupCourseId(id, groupCourseId)
        .map(mapper::toDomain)
        .orElseThrow(() -> notFound(id));
  }

  public Exam create(UUID groupCourseId, Exam exam) {
    JGroupCourse groupCourse = findGroupCourse(groupCourseId);
    validateTotalCoefficient(groupCourseId, exam.coefficient(), null);
    return mapper.toDomain(repository.save(mapper.toEntity(exam, groupCourse)));
  }

  public Exam update(UUID groupCourseId, UUID id, Exam exam) {
    JGroupCourse groupCourse = findGroupCourse(groupCourseId);
    if (!repository.existsByIdAndGroupCourseId(id, groupCourseId)) {
      throw notFound(id);
    }
    validateTotalCoefficient(groupCourseId, exam.coefficient(), id);
    var withId = new Exam(id, exam.examDate(), exam.coefficient(), groupCourseId);
    JExam saved = repository.save(mapper.toEntity(withId, groupCourse));
    return mapper.toDomain(saved);
  }

  public void delete(UUID groupCourseId, UUID id) {
    if (!repository.existsByIdAndGroupCourseId(id, groupCourseId)) {
      throw notFound(id);
    }
    repository.deleteById(id);
  }

  private void validateTotalCoefficient(
      UUID groupCourseId, BigDecimal newCoefficient, UUID excludeExamId) {
    BigDecimal existingTotal =
        repository.findByGroupCourseId(groupCourseId).stream()
            .filter(e -> excludeExamId == null || !e.getId().equals(excludeExamId))
            .map(JExam::getCoefficient)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal total = existingTotal.add(newCoefficient);
    if (total.compareTo(MAX_TOTAL_COEFFICIENT) > 0) {
      throw new BadRequestException(
          "Total exam coefficient for this group course would be " + total + ", cannot exceed 1");
    }
  }

  private JGroupCourse findGroupCourse(UUID groupCourseId) {
    return groupCourseRepository
        .findById(groupCourseId)
        .orElseThrow(() -> new NotFoundException("Group course not found: " + groupCourseId));
  }

  private static NotFoundException notFound(UUID id) {
    return new NotFoundException("Exam not found: " + id);
  }
}
