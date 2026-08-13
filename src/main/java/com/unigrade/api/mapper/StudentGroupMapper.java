package com.unigrade.api.mapper;

import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupMapper {

  public StudentGroup toDomain(JStudentGroup entity) {
    return new StudentGroup(entity.getId(), entity.getReference(), entity.getPromotion().getId());
  }

  public JStudentGroup toEntity(StudentGroup domain, JPromotion promotion) {
    var entity = new JStudentGroup();
    entity.setId(domain.id());
    entity.setReference(domain.reference());
    entity.setPromotion(promotion);
    return entity;
  }
}
