package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.Promotion;
import com.unigrade.api.repository.model.JPromotion;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromotionMapperTest {
  private final PromotionMapper promotionMapper = new PromotionMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var entity = new JPromotion();
    entity.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    entity.setReference("PROMO-2026");
    entity.setStartYear((short) 2026);
    entity.setEndYear((short) 2029);

    Promotion result = promotionMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getReference(), result.reference());
    assertEquals(entity.getStartYear(), result.startYear());
    assertEquals(entity.getEndYear(), result.endYear());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new Promotion(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "PROMO-2026",
            (short) 2026,
            (short) 2029);

    JPromotion result = promotionMapper.toEntity(domain);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.reference(), result.getReference());
    assertEquals(domain.startYear(), result.getStartYear());
    assertEquals(domain.endYear(), result.getEndYear());
  }
}
