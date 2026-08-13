package com.unigrade.api.mapper;

import com.unigrade.api.model.Course;
import com.unigrade.api.repository.model.JCourse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public Course toDomain(JCourse entity) {
    return Course.builder()
        .id(entity.getId())
        .reference(entity.getReference())
        .title(entity.getTitle())
        .credits(entity.getCredits())
        .build();
  }

  public JCourse toEntity(Course domain) {
    return JCourse.builder()
        .id(domain.id())
        .reference(domain.reference())
        .title(domain.title())
        .credits(domain.credits())
        .build();
  }
}
