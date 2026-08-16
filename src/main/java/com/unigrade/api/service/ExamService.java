package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.model.dto.ExamRequest;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamService {

  private static final int COEFFICIENT_SCALE = 4;

  private final ExamRepository repository;
  private final GroupCourseRepository groupCourseRepository;
  private final GradeRepository gradeRepository;
  private final ExamMapper mapper;

  public List<Exam> findByGroupAndCourse(UUID groupId, UUID courseId) {
    JGroupCourse assignment = resolveActiveAssignment(groupId, courseId);
    return repository.findByGroupCourseIdOrderByExamDateAsc(assignment.getId()).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public Exam findById(UUID groupId, UUID courseId, UUID examId) {
    JGroupCourse assignment = resolveActiveAssignment(groupId, courseId);
    return mapper.toDomain(resolveExam(assignment, examId));
  }

  @Transactional
  public Exam create(UUID groupId, UUID courseId, ExamRequest request) {
    JGroupCourse assignment = resolveActiveAssignmentForUpdate(groupId, courseId);
    checkExamBeforeCourseEnd(assignment, request.examDate());

    BigDecimal coefficient = normalize(request.coefficient());
    checkCoefficientTotal(
        repository.sumCoefficientByGroupCourseId(assignment.getId()).add(coefficient));

    var exam = new Exam(null, assignment.getId(), request.examDate(), coefficient);
    return mapper.toDomain(repository.save(mapper.toEntity(exam, assignment)));
  }

  @Transactional
  public Exam update(UUID groupId, UUID courseId, UUID examId, ExamRequest request) {
    JGroupCourse assignment = resolveActiveAssignmentForUpdate(groupId, courseId);
    JExam exam = resolveExam(assignment, examId);
    checkExamBeforeCourseEnd(assignment, request.examDate());

    BigDecimal coefficient = normalize(request.coefficient());
    checkCoefficientTotal(
        repository
            .sumCoefficientByGroupCourseIdExcluding(assignment.getId(), examId)
            .add(coefficient));

    exam.setExamDate(request.examDate());
    exam.setCoefficient(coefficient);
    return mapper.toDomain(repository.save(exam));
  }

  @Transactional
  public void delete(UUID groupId, UUID courseId, UUID examId) {
    JGroupCourse assignment = resolveActiveAssignment(groupId, courseId);
    JExam exam = resolveExam(assignment, examId);
    if (gradeRepository.existsByExamId(examId)) {
      throw new ConflictException("Cannot delete: grades exist for this exam");
    }
    repository.delete(exam);
  }

  private JGroupCourse resolveActiveAssignment(UUID groupId, UUID courseId) {
    return groupCourseRepository
        .findByGroupIdAndCourseIdAndEndDateIsNull(groupId, courseId)
        .orElseThrow(() -> noActiveAssignment(groupId, courseId));
  }

  private JGroupCourse resolveActiveAssignmentForUpdate(UUID groupId, UUID courseId) {
    return groupCourseRepository
        .findActiveForUpdate(groupId, courseId)
        .orElseThrow(() -> noActiveAssignment(groupId, courseId));
  }

  private JExam resolveExam(JGroupCourse assignment, UUID examId) {
    return repository
        .findByIdAndGroupCourseId(examId, assignment.getId())
        .orElseThrow(() -> new NotFoundException("Exam not found: " + examId));
  }

  private void checkCoefficientTotal(BigDecimal total) {
    if (total.compareTo(BigDecimal.ONE) > 0) {
      throw new BadRequestException(
          "Course exams coefficient total exceed 1. Current = %s".formatted(total.toPlainString()));
    }
  }

  private void checkExamBeforeCourseEnd(JGroupCourse assignment, Instant examDate) {
    LocalDate date = examDate.atZone(ZoneOffset.UTC).toLocalDate();
    if (date.isBefore(assignment.getStartDate())) {
      throw new BadRequestException(
          "Exam date must not be before the course assignment start date");
    }
    if (assignment.getEndDate() != null && date.isAfter(assignment.getEndDate())) {
      throw new BadRequestException("Exam date must not be after the course assignment end date");
    }
  }

  private BigDecimal normalize(BigDecimal coefficient) {
    return coefficient.setScale(COEFFICIENT_SCALE, RoundingMode.HALF_UP);
  }

  private NotFoundException noActiveAssignment(UUID groupId, UUID courseId) {
    return new NotFoundException(
        "No active course assignment for course " + courseId + " in group " + groupId);
  }
}
