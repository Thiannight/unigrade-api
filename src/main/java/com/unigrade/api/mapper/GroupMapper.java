package com.unigrade.api.mapper;

import com.unigrade.api.model.Group;
import com.unigrade.api.repository.model.JGroup;
import com.unigrade.api.repository.model.JPromotion;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

  public Group toDomain(JGroup entity) {
    return Group.builder()
        .id(entity.getId())
        .reference(entity.getReference())
        .promotionId(entity.getPromotion().getId())
        .build();
  }

  public JGroup toEntity(Group domain, JPromotion promotion) {
    return JGroup.builder()
        .id(domain.id())
        .reference(domain.reference())
        .promotion(promotion)
        .build();
  }
}
