package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.Group;
import com.unigrade.api.repository.model.JGroup;
import com.unigrade.api.repository.model.JPromotion;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupMapperTest {
  private final GroupMapper groupMapper = new GroupMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    JPromotion promotion =
        JPromotion.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();
    JGroup entity =
        JGroup.builder()
            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .reference("A1")
            .promotion(promotion)
            .build();

    Group result = groupMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getReference(), result.reference());
    assertEquals(entity.getPromotion().getId(), result.promotionId());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    Group domain =
        Group.builder()
            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .reference("A1")
            .promotionId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .build();
    JPromotion promotion =
        JPromotion.builder().id(UUID.fromString("11111111-1111-1111-1111-111111111111")).build();

    JGroup result = groupMapper.toEntity(domain, promotion);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.reference(), result.getReference());
    assertEquals(promotion, result.getPromotion());
  }
}
