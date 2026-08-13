package com.unigrade.api.mapper;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.repository.model.JPromotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

  public Promotion toDomain(JPromotion entity) {
    return Promotion.builder()
        .id(entity.getId())
        .reference(entity.getReference())
        .startYear(entity.getStartYear())
        .endYear(entity.getEndYear())
        .build();
  }

  public JPromotion toEntity(Promotion domain) {
    var entity = new JPromotion();
    entity.setId(domain.id());
    entity.setReference(domain.reference());
    entity.setStartYear(domain.startYear());
    entity.setEndYear(domain.endYear());
    return entity;
  }
}
