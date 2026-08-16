package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.unigrade.api.model.GroupCourse;
import com.unigrade.api.model.Semester;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JStudentGroup;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupCourseMapperTest {
  private final GroupCourseMapper groupCourseMapper = new GroupCourseMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var course = new JCourse();
    course.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var group = new JStudentGroup();
    group.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    var entity = new JGroupCourse();
    entity.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    entity.setCourse(course);
    entity.setGroup(group);
    entity.setStartDate(LocalDate.of(2024, 1, 1));
    entity.setEndDate(null);
    entity.setSemester(Semester.S3);

    GroupCourse result = groupCourseMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getCourse().getId(), result.courseId());
    assertEquals(entity.getGroup().getId(), result.groupId());
    assertEquals(entity.getSemester(), result.semester());
    assertEquals(entity.getStartDate(), result.startDate());
    assertNull(result.endDate());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new GroupCourse(
            UUID.fromString("44444444-4444-4444-4444-444444444444"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            Semester.S3,
            LocalDate.of(2024, 1, 1),
            null);
    var course = new JCourse();
    course.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var group = new JStudentGroup();
    group.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    JGroupCourse result = groupCourseMapper.toEntity(domain, course, group);

    assertEquals(domain.id(), result.getId());
    assertEquals(course, result.getCourse());
    assertEquals(group, result.getGroup());
    assertEquals(domain.semester(), result.getSemester());
    assertEquals(domain.startDate(), result.getStartDate());
    assertNull(result.getEndDate());
  }
}
