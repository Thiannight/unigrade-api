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
        .courseId(entity.getCourse().getId())
        .coefficient(entity.getCoefficient())
        .build();
  }

  public JExam toEntity(Exam domain, JCourse course) {
    return JExam.builder()
        .id(domain.id())
        .examDate(domain.examDate())
        .coefficient(domain.coefficient())
        .course(course)
        .build();
  }
}
