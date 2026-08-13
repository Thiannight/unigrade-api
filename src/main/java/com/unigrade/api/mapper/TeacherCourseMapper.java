package com.unigrade.api.mapper;

import com.unigrade.api.model.TeacherCourse;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JTeacherCourse;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class TeacherCourseMapper {

  public TeacherCourse toDomain(JTeacherCourse entity) {
    return new TeacherCourse(
        entity.getId(),
        entity.getCourse().getId(),
        entity.getTeacher().getId(),
        entity.getSchoolYear());
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
