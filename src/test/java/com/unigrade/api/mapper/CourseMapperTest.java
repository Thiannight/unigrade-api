package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.Course;
import com.unigrade.api.repository.model.JCourse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseMapperTest {
  private final CourseMapper courseMapper = new CourseMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var entity = new JCourse();
    entity.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    entity.setReference("CS101");
    entity.setTitle("Intro to CS");
    entity.setCredits((short) 6);

    Course result = courseMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getReference(), result.reference());
    assertEquals(entity.getTitle(), result.title());
    assertEquals(entity.getCredits(), result.credits());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new Course(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "CS101",
            "Intro to CS",
            (short) 6);

    JCourse result = courseMapper.toEntity(domain);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.reference(), result.getReference());
    assertEquals(domain.title(), result.getTitle());
    assertEquals(domain.credits(), result.getCredits());
  }
}
