package com.unigrade.api.mapper;

import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

  public Exam toDomain(JExam entity) {
    return Exam.builder()
        .id(entity.getId())
        .examDate(entity.getExamDate())
        .coefficient(entity.getCoefficient())
        .groupCourseId(entity.getGroupCourse().getId())
        .build();
  }

  public JExam toEntity(Exam domain, JGroupCourse groupCourse) {
    return JExam.builder()
        .id(domain.id())
        .examDate(domain.examDate())
        .coefficient(domain.coefficient())
        .groupCourse(groupCourse)
        .build();
  }
}
