package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExamMapperTest {
  private final ExamMapper examMapper = new ExamMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    JCourse course =
        JCourse.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
    JExam entity =
        JExam.builder()
            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .examDate(Instant.parse("2024-01-01T10:00:00Z"))
            .coefficient(new BigDecimal("0.5"))
            .course(course)
            .build();

    Exam result = examMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getExamDate(), result.examDate());
    assertEquals(entity.getCourse().getId(), result.courseId());
    assertEquals(entity.getCoefficient(), result.coefficient());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    Exam domain =
        Exam.builder()
            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .examDate(Instant.parse("2024-01-01T10:00:00Z"))
            .courseId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .coefficient(new BigDecimal("0.5"))
            .build();
    JCourse course =
        JCourse.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();

    JExam result = examMapper.toEntity(domain, course);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.examDate(), result.getExamDate());
    assertEquals(domain.coefficient(), result.getCoefficient());
    assertEquals(course, result.getCourse());
  }
}
