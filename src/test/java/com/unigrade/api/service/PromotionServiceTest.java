package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
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

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private PromotionRepository repository;
  private final PromotionMapper mapper = new PromotionMapper();
  private PromotionService service;

  @BeforeEach
  void setUp() {
    service = new PromotionService(repository, mapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(repository.findAll()).thenReturn(List.of(entity()));

    List<Promotion> result = service.findAll();

    assertEquals(1, result.size());
    assertEquals("PROMO-2026", result.get(0).reference());
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findById(ID)).thenReturn(Optional.of(entity()));

    Promotion result = service.findById(ID);

    assertEquals(ID, result.id());
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.findById(ID));

    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void create_validYears_saves() {
    var domain = new Promotion(null, "PROMO-2026", (short) 2026, (short) 2029);
    when(repository.save(any())).thenReturn(entity());

    Promotion result = service.create(domain);

    assertEquals("PROMO-2026", result.reference());
    verify(repository).save(any());
  }

  @Test
  void create_invalidYears_failsBeanValidation() {
    var domain = new Promotion(null, "PROMO-2026", (short) 2029, (short) 2026);

    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var violations = validatorFactory.getValidator().validate(domain);

      assertFalse(violations.isEmpty());
    }
  }

  @Test
  void create_duplicateConstraint_throwsConflict() {
    var domain = new Promotion(null, "PROMO-2026", (short) 2026, (short) 2029);
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(ConflictException.class, () -> service.create(domain));
  }

  @Test
  void update_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);
    var domain = new Promotion(ID, "PROMO-2026", (short) 2026, (short) 2029);

    assertThrows(NotFoundException.class, () -> service.update(ID, domain));
  }

  @Test
  void update_existing_saves() {
    when(repository.existsById(ID)).thenReturn(true);
    when(repository.save(any())).thenReturn(entity());
    var domain = new Promotion(ID, "PROMO-2026", (short) 2026, (short) 2029);

    Promotion result = service.update(ID, domain);

    assertEquals(ID, result.id());
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.delete(ID));
  }

  @Test
  void delete_existing_deletes() {
    when(repository.existsById(ID)).thenReturn(true);

    service.delete(ID);

    verify(repository).deleteById(ID);
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
