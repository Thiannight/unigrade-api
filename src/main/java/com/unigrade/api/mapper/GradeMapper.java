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
    return JGrade.builder()
        .id(domain.id())
        .score(domain.score())
        .gradeDate(domain.gradeDate())
        .reason(domain.reason())
        .student(student)
        .exam(exam)
        .build();
  }
}
