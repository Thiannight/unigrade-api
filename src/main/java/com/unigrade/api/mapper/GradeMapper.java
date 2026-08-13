package com.unigrade.api.mapper;

import com.unigrade.api.model.Grade;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

  public Grade toDomain(JGrade entity) {
    return Grade.builder()
        .id(entity.getId())
        .score(entity.getScore())
        .gradeDate(entity.getGradeDate())
        .reason(entity.getReason())
        .studentId(entity.getStudent().getId())
        .examId(entity.getExam().getId())
        .build();
  }

  public JGrade toEntity(Grade domain, JUser student, JExam exam) {
    var entity = new JGrade();
    entity.setId(domain.id());
    entity.setScore(domain.score());
    entity.setGradeDate(domain.gradeDate());
    entity.setReason(domain.reason());
    entity.setStudent(student);
    entity.setExam(exam);
    return entity;
  }
}
