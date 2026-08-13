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
    var entity = new JCourse();
    entity.setId(domain.id());
    entity.setReference(domain.reference());
    entity.setTitle(domain.title());
    entity.setCredits(domain.credits());
    return entity;
  }
}
