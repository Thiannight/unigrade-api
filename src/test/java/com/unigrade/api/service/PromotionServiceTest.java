package com.unigrade.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.mapper.PromotionMapper;
import com.unigrade.api.model.Promotion;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.model.JPromotion;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private PromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper = new PromotionMapper();
  private PromotionService promotionService;

  @BeforeEach
  void setUp() {
    promotionService = new PromotionService(promotionRepository, promotionMapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(promotionRepository.findAll()).thenReturn(List.of(entity()));

    List<Promotion> result = promotionService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).reference()).isEqualTo("PROMO-2026");
  }

  @Test
  void findById_existing_returnsMapped() {
    when(promotionRepository.findById(ID)).thenReturn(Optional.of(entity()));

    Promotion result = promotionService.findById(ID);

    assertThat(result.id()).isEqualTo(ID);
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(promotionRepository.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> promotionService.findById(ID))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void create_validYears_saves() {
    var domain = new Promotion(null, "PROMO-2026", (short) 2026, (short) 2029);
    when(promotionRepository.save(any())).thenReturn(entity());

    Promotion result = promotionService.create(domain);

    assertThat(result.reference()).isEqualTo("PROMO-2026");
    verify(promotionRepository).save(any());
  }

  @Test
  void create_invalidYears_failsBeanValidation() {
    var domain = new Promotion(null, "PROMO-2026", (short) 2029, (short) 2026);

    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var violations = validatorFactory.getValidator().validate(domain);

      assertThat(violations).isNotEmpty();
    }
  }

  @Test
  void create_duplicateConstraint_throwsConflict() {
    var domain = new Promotion(null, "PROMO-2026", (short) 2026, (short) 2029);
    when(promotionRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThatThrownBy(() -> promotionService.create(domain))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");
  }

  @Test
  void update_missing_throwsNotFound() {
    when(promotionRepository.existsById(ID)).thenReturn(false);
    var domain = new Promotion(ID, "PROMO-2026", (short) 2026, (short) 2029);

    assertThatThrownBy(() -> promotionService.update(ID, domain))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void update_existing_saves() {
    when(promotionRepository.existsById(ID)).thenReturn(true);
    when(promotionRepository.save(any())).thenReturn(entity());
    var domain = new Promotion(ID, "PROMO-2026", (short) 2026, (short) 2029);

    Promotion result = promotionService.update(ID, domain);

    assertThat(result.id()).isEqualTo(ID);
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(promotionRepository.existsById(ID)).thenReturn(false);

    assertThatThrownBy(() -> promotionService.delete(ID))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void delete_existing_deletes() {
    when(promotionRepository.existsById(ID)).thenReturn(true);

    promotionService.delete(ID);

    verify(promotionRepository).deleteById(ID);
  }

  private JPromotion entity() {
    var e = new JPromotion();
    e.setId(ID);
    e.setReference("PROMO-2026");
    e.setStartYear((short) 2026);
    e.setEndYear((short) 2029);
    return e;
  }
}
