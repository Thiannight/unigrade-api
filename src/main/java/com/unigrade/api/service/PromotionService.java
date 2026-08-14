package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.PromotionMapper;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.model.JPromotion;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromotionService {

  private final PromotionRepository repository;
  private final PromotionMapper mapper;

  public List<Promotion> findAll() {
    return repository.findAll().stream().map(mapper::toDomain).toList();
  }

  public Promotion findById(UUID id) throws NotFoundException {
    return repository.findById(id).map(mapper::toDomain).orElseThrow(() -> notFound(id));
  }

  public Promotion create(Promotion promotion) throws ConflictException {
    return saveAndMap(mapper.toEntity(promotion));
  }

  public Promotion update(UUID id, Promotion promotion)
      throws NotFoundException, ConflictException {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    var withId =
        new Promotion(id, promotion.reference(), promotion.startYear(), promotion.endYear());
    return saveAndMap(mapper.toEntity(withId));
  }

  public void delete(UUID id) throws NotFoundException {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    repository.deleteById(id);
  }

  private Promotion saveAndMap(JPromotion entity) throws ConflictException {
    try {
      return mapper.toDomain(repository.save(entity));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Promotion reference, start year or end year already exists");
    }
  }

  private static NotFoundException notFound(UUID id) {
    return new NotFoundException("Promotion not found: " + id);
  }
}
