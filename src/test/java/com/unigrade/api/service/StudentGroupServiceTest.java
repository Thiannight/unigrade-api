package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.StudentGroupMapper;
import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.StudentGroupRepository;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class StudentGroupServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID PROMOTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Mock private StudentGroupRepository repository;
  @Mock private PromotionRepository promotionRepository;
  private final StudentGroupMapper mapper = new StudentGroupMapper();
  private StudentGroupService service;

  @BeforeEach
  void setUp() {
    service = new StudentGroupService(repository, promotionRepository, mapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity())));

    List<StudentGroup> result = service.findAll(1, 10);

    assertEquals(1, result.size());
    assertEquals("A1", result.get(0).reference());
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findById(ID)).thenReturn(Optional.of(entity()));

    StudentGroup result = service.findById(ID);

    assertEquals(ID, result.id());
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findById(ID));

    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void create_saves() {
    var domain = new StudentGroup(null, "A1", PROMOTION_ID);
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(repository.save(any())).thenReturn(entity());

    StudentGroup result = service.create(domain);

    assertEquals("A1", result.reference());
    verify(repository).save(any());
  }

  @Test
  void create_missingPromotion_throwsNotFound() {
    var domain = new StudentGroup(null, "A1", PROMOTION_ID);
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.create(domain));

    assertTrue(exception.getMessage().contains("Promotion not found"));
  }

  @Test
  void create_duplicateConstraint_throwsConflict() {
    var domain = new StudentGroup(null, "A1", PROMOTION_ID);
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(ConflictException.class, () -> service.create(domain));
  }

  @Test
  void update_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);
    var domain = new StudentGroup(ID, "A1", PROMOTION_ID);

    assertThrows(NotFoundException.class, () -> service.update(ID, domain));
  }

  @Test
  void update_existing_saves() {
    when(repository.existsById(ID)).thenReturn(true);
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.of(promotion()));
    when(repository.save(any())).thenReturn(entity());
    var domain = new StudentGroup(ID, "A1", PROMOTION_ID);

    StudentGroup result = service.update(ID, domain);

    assertEquals(ID, result.id());
  }

  @Test
  void update_missingPromotion_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(true);
    when(promotionRepository.findById(PROMOTION_ID)).thenReturn(Optional.empty());
    var domain = new StudentGroup(ID, "A1", PROMOTION_ID);

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.update(ID, domain));

    assertTrue(exception.getMessage().contains("Promotion not found"));
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

  private JPromotion promotion() {
    var promotion = new JPromotion();
    promotion.setId(PROMOTION_ID);
    return promotion;
  }

  private JStudentGroup entity() {
    var entity = new JStudentGroup();
    entity.setId(ID);
    entity.setReference("A1");
    entity.setPromotion(promotion());
    return entity;
  }
}
