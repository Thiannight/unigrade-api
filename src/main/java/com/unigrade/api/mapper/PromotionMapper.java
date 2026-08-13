package com.unigrade.api.mapper;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.repository.model.JPromotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

  public Promotion toDomain(JPromotion entity) {
    return new Promotion(
        entity.getId(), entity.getReference(), entity.getStartYear(), entity.getEndYear());
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
