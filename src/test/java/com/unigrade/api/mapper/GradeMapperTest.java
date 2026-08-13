package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.Grade;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradeMapperTest {
  private final GradeMapper gradeMapper = new GradeMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var student = new JUser();
    student.setId("STD00001");
    var exam = new JExam();
    exam.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    var entity = new JGrade();
    entity.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    entity.setScore(15.5f);
    entity.setGradeDate(Instant.parse("2026-01-01T10:00:00Z"));
    entity.setReason("Midterm");
    entity.setStudent(student);
    entity.setExam(exam);

    Grade result = gradeMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getScore(), result.score());
    assertEquals(entity.getGradeDate(), result.gradeDate());
    assertEquals(entity.getReason(), result.reason());
    assertEquals(entity.getStudent().getId(), result.studentId());
    assertEquals(entity.getExam().getId(), result.examId());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new Grade(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            15.5f,
            Instant.parse("2026-01-01T10:00:00Z"),
            "Midterm",
            "STD00001",
            UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var student = new JUser();
    student.setId("STD00001");
    var exam = new JExam();
    exam.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    JGrade result = gradeMapper.toEntity(domain, student, exam);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.score(), result.getScore());
    assertEquals(domain.gradeDate(), result.getGradeDate());
    assertEquals(domain.reason(), result.getReason());
    assertEquals(student, result.getStudent());
    assertEquals(exam, result.getExam());
  }
}
