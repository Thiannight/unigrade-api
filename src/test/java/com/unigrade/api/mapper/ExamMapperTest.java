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
    var course = new JCourse();
    course.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    var entity = new JExam();
    entity.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    entity.setExamDate(Instant.parse("2026-01-01T10:00:00Z"));
    entity.setCoefficient(new BigDecimal("0.5000"));
    entity.setCourse(course);

    Exam result = examMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getExamDate(), result.examDate());
    assertEquals(entity.getCoefficient(), result.coefficient());
    assertEquals(entity.getCourse().getId(), result.courseId());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new Exam(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            Instant.parse("2026-01-01T10:00:00Z"),
            new BigDecimal("0.5000"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var course = new JCourse();
    course.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    JExam result = examMapper.toEntity(domain, course);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.examDate(), result.getExamDate());
    assertEquals(domain.coefficient(), result.getCoefficient());
    assertEquals(course, result.getCourse());
  }
}
