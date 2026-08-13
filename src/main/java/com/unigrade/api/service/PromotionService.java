package com.unigrade.api.service;

import com.unigrade.api.mapper.PromotionMapper;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.model.JPromotion;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PromotionService {

  private final PromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper;

  public List<Promotion> findAll() {
    return promotionRepository.findAll().stream().map(promotionMapper::toDomain).toList();
  }

  public Promotion findById(UUID id) {
    return promotionRepository
        .findById(id)
        .map(promotionMapper::toDomain)
        .orElseThrow(() -> notFound(id));
  }

  public Promotion create(Promotion promotion) {
    validateYears(promotion);
    var withoutId =
        new Promotion(null, promotion.reference(), promotion.startYear(), promotion.endYear());
    return saveAndMap(promotionMapper.toEntity(withoutId));
  }

  public Promotion update(UUID id, Promotion promotion) {
    validateYears(promotion);
    if (!promotionRepository.existsById(id)) {
      throw notFound(id);
    }
    var withId =
        new Promotion(id, promotion.reference(), promotion.startYear(), promotion.endYear());
    return saveAndMap(promotionMapper.toEntity(withId));
  }

  public void delete(UUID id) {
    if (!promotionRepository.existsById(id)) {
      throw notFound(id);
    }
    promotionRepository.deleteById(id);
  }

  private Promotion saveAndMap(JPromotion entity) {
    try {
      return promotionMapper.toDomain(promotionRepository.save(entity));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Promotion reference, start year or end year already exists", e);
    }
  }

  private void validateYears(Promotion promotion) {
    if (promotion.startYear() >= promotion.endYear()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "startYear must be strictly before endYear");
    }
  }

  private static ResponseStatusException notFound(UUID id) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found: " + id);
  }
}
