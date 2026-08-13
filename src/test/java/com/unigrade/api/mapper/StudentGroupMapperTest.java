package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudentGroupMapperTest {
  private final StudentGroupMapper studentGroupMapper = new StudentGroupMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    var promotion = new JPromotion();
    promotion.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    var entity = new JStudentGroup();
    entity.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    entity.setReference("A1");
    entity.setPromotion(promotion);

    StudentGroup result = studentGroupMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getReference(), result.reference());
    assertEquals(entity.getPromotion().getId(), result.promotionId());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new StudentGroup(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "A1",
            UUID.fromString("11111111-1111-1111-1111-111111111111"));
    var promotion = new JPromotion();
    promotion.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    JStudentGroup result = studentGroupMapper.toEntity(domain, promotion);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.reference(), result.getReference());
    assertEquals(promotion, result.getPromotion());
  }
}
