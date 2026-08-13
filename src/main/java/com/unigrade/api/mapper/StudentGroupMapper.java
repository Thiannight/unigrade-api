package com.unigrade.api.mapper;

import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupMapper {

  public StudentGroup toDomain(JStudentGroup entity) {
    return StudentGroup.builder()
        .id(entity.getId())
        .reference(entity.getReference())
        .promotionId(entity.getPromotion().getId())
        .build();
  }

  public JStudentGroup toEntity(StudentGroup domain, JPromotion promotion) {
    return JStudentGroup.builder()
        .id(domain.id())
        .reference(domain.reference())
        .promotion(promotion)
        .build();
  }
}
