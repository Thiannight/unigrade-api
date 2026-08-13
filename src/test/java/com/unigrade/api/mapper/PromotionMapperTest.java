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
    JPromotion entity =
        JPromotion.builder()
            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .reference("PROM1")
            .startYear((short) 2024)
            .endYear((short) 2028)
            .build();

    Promotion result = promotionMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getReference(), result.reference());
    assertEquals(entity.getStartYear(), result.startYear());
    assertEquals(entity.getEndYear(), result.endYear());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    Promotion domain =
        Promotion.builder()
            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .reference("PROM1")
            .startYear((short) 2024)
            .endYear((short) 2028)
            .build();

    JPromotion result = promotionMapper.toEntity(domain);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.reference(), result.getReference());
    assertEquals(domain.startYear(), result.getStartYear());
    assertEquals(domain.endYear(), result.getEndYear());
  }
}
