package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.StudentGroupMapper;
import com.unigrade.api.model.StudentGroup;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.PromotionRepository;
import com.unigrade.api.repository.StudentGroupRepository;
import com.unigrade.api.repository.model.JPromotion;
import com.unigrade.api.repository.model.JStudentGroup;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentGroupService {

  private final StudentGroupRepository repository;
  private final GroupCourseRepository groupCourseRepository;
  private final PromotionRepository promotionRepository;
  private final StudentGroupMapper mapper;

  public List<StudentGroup> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 20)))
        .map(mapper::toDomain)
        .toList();
  }

  public StudentGroup findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain).orElseThrow(() -> notFound(id));
  }

  public StudentGroup create(StudentGroup group) {
    return mapper.toDomain(
        raceAwareSave(mapper.toEntity(group, resolvePromotion(group.promotionId()))));
  }

  public StudentGroup update(UUID id, StudentGroup group) {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    var withId = new StudentGroup(id, group.reference(), group.promotionId());
    return mapper.toDomain(
        raceAwareSave(mapper.toEntity(withId, resolvePromotion(withId.promotionId()))));
  }

  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw notFound(id);
    }
    if (groupCourseRepository.existsByGroupId(id)) {
      throw new ConflictException("Cannot delete: group has course assignments");
    }
    repository.deleteById(id);
  }

  private JStudentGroup raceAwareSave(JStudentGroup group) {
    try {
      return repository.save(group);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Group reference already exists in this promotion");
    }
  }

  private JPromotion resolvePromotion(UUID promotionId) {
    return promotionRepository
        .findById(promotionId)
        .orElseThrow(() -> new NotFoundException("Promotion not found: " + promotionId));
  }

  private static NotFoundException notFound(UUID id) {
    return new NotFoundException("Student group not found: " + id);
  }
}
