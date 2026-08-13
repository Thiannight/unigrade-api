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
        .schoolYear(entity.getSchoolYear())
        .build();
  }

  public JTeacherCourse toEntity(TeacherCourse domain, JCourse course, JUser teacher) {
    var entity = new JTeacherCourse();
    entity.setId(domain.id());
    entity.setCourse(course);
    entity.setTeacher(teacher);
    entity.setSchoolYear(domain.schoolYear());
    return entity;
  }
}
