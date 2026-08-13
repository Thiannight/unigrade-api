package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.TeacherCourse;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JTeacherCourse;
import com.unigrade.api.repository.model.JUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeacherCourseMapperTest {
  private final TeacherCourseMapper teacherCourseMapper = new TeacherCourseMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var course = new JCourse();
    course.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var teacher = new JUser();
    teacher.setId("TCR00001");

    var entity = new JTeacherCourse();
    entity.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    entity.setCourse(course);
    entity.setTeacher(teacher);
    entity.setSchoolYear((short) 2026);

    TeacherCourse result = teacherCourseMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getCourse().getId(), result.courseId());
    assertEquals(entity.getTeacher().getId(), result.teacherId());
    assertEquals(entity.getSchoolYear(), result.schoolYear());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new TeacherCourse(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "TCR00001",
            (short) 2026);
    var course = new JCourse();
    course.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var teacher = new JUser();
    teacher.setId("TCR00001");

    JTeacherCourse result = teacherCourseMapper.toEntity(domain, course, teacher);

    assertEquals(domain.id(), result.getId());
    assertEquals(course, result.getCourse());
    assertEquals(teacher, result.getTeacher());
    assertEquals(domain.schoolYear(), result.getSchoolYear());
  }
}
