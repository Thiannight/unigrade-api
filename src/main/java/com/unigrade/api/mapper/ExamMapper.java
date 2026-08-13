package com.unigrade.api.mapper;

import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

  public Exam toDomain(JExam entity) {
    return Exam.builder()
        .id(entity.getId())
        .examDate(entity.getExamDate())
        .coefficient(entity.getCoefficient())
        .courseId(entity.getCourse().getId())
        .build();
  }

  public JExam toEntity(Exam domain, JCourse course) {
    var entity = new JExam();
    entity.setId(domain.id());
    entity.setExamDate(domain.examDate());
    entity.setCoefficient(domain.coefficient());
    entity.setCourse(course);
    return entity;
  }
}
