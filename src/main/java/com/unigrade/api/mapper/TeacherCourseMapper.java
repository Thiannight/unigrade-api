package com.unigrade.api.mapper;

import com.unigrade.api.model.TeacherCourse;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JTeacherCourse;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class TeacherCourseMapper {

  public TeacherCourse toDomain(JTeacherCourse entity) {
    return TeacherCourse.builder()
        .id(entity.getId())
        .courseId(entity.getCourse().getId())
        .teacherId(entity.getTeacher().getId())
        .priority(entity.getPriority())
        .build();
  }

  public JTeacherCourse toEntity(TeacherCourse domain, JCourse course, JUser teacher) {
    return JTeacherCourse.builder()
        .id(domain.id())
        .course(course)
        .teacher(teacher)
        .priority(domain.priority())
        .build();
  }
}
