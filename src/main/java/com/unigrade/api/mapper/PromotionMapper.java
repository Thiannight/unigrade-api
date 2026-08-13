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
    return JPromotion.builder()
        .id(domain.id())
        .reference(domain.reference())
        .startYear(domain.startYear())
        .endYear(domain.endYear())
        .build();
  }
}
