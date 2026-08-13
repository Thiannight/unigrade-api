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
    JUser student = JUser.builder().id("STD00001").build();
    JExam exam =
        JExam.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
    JGrade entity =
        JGrade.builder()
            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .score(15.5f)
            .gradeDate(Instant.parse("2024-01-01T10:00:00Z"))
            .reason("Initial grade")
            .student(student)
            .exam(exam)
            .build();

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
    Grade domain =
        Grade.builder()
            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .score(15.5f)
            .gradeDate(Instant.parse("2024-01-01T10:00:00Z"))
            .reason("Initial grade")
            .studentId("STD00001")
            .examId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .build();
    JUser student = JUser.builder().id("STD00001").build();
    JExam exam =
        JExam.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();

    JGrade result = gradeMapper.toEntity(domain, student, exam);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.score(), result.getScore());
    assertEquals(domain.gradeDate(), result.getGradeDate());
    assertEquals(domain.reason(), result.getReason());
    assertEquals(student, result.getStudent());
    assertEquals(exam, result.getExam());
  }
}
