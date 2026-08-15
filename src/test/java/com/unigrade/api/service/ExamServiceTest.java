package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGroupCourse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID GROUP_COURSE_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final Instant EXAM_DATE = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private ExamRepository repository;
  @Mock private GroupCourseRepository groupCourseRepository;
  private final ExamMapper mapper = new ExamMapper();
  private ExamService service;

  @BeforeEach
  void setUp() {
    service = new ExamService(repository, groupCourseRepository, mapper);
  }

  @Test
  void findAll_existingGroupCourse_returnsMappedList() {
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.findAllByGroupCourseId(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity(ID, new BigDecimal("0.5000")))));

    List<Exam> result = service.findAll(GROUP_COURSE_ID, 0, 10);

    assertEquals(1, result.size());
    assertEquals(GROUP_COURSE_ID, result.get(0).groupCourseId());
  }

  @Test
  void findAll_missingGroupCourse_throwsNotFound() {
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.findAll(GROUP_COURSE_ID, 0, 10));
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findByIdAndGroupCourseId(ID, GROUP_COURSE_ID))
        .thenReturn(Optional.of(entity(ID, new BigDecimal("0.5000"))));

    Exam result = service.findById(GROUP_COURSE_ID, ID);

    assertEquals(ID, result.id());
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findByIdAndGroupCourseId(ID, GROUP_COURSE_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.findById(GROUP_COURSE_ID, ID));

    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void create_underTotal_saves() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("0.5000"), GROUP_COURSE_ID);
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.findByGroupCourseId(GROUP_COURSE_ID)).thenReturn(List.of());
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("0.5000")));

    Exam result = service.create(GROUP_COURSE_ID, domain);

    assertEquals(GROUP_COURSE_ID, result.groupCourseId());
    verify(repository).save(any());
  }

  @Test
  void create_missingGroupCourse_throwsNotFound() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("0.5000"), GROUP_COURSE_ID);
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(GROUP_COURSE_ID, domain));
  }

  @Test
  void create_totalExceeds1_throwsBadRequest() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("0.6000"), GROUP_COURSE_ID);
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.findByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(List.of(entity(OTHER_ID, new BigDecimal("0.5000"))));

    assertThrows(BadRequestException.class, () -> service.create(GROUP_COURSE_ID, domain));
  }

  @Test
  void create_totalExactly1_saves() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("0.5000"), GROUP_COURSE_ID);
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.findByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(List.of(entity(OTHER_ID, new BigDecimal("0.5000"))));
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("0.5000")));

    Exam result = service.create(GROUP_COURSE_ID, domain);

    assertEquals(ID, result.id());
  }

  @Test
  void update_excludesItselfFromTotal_saves() {
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.existsByIdAndGroupCourseId(ID, GROUP_COURSE_ID)).thenReturn(true);
    when(repository.findByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(
            List.of(
                entity(ID, new BigDecimal("0.5000")), entity(OTHER_ID, new BigDecimal("0.3000"))));
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("0.7000")));
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.7000"), GROUP_COURSE_ID);

    Exam result = service.update(GROUP_COURSE_ID, ID, domain);

    assertEquals(ID, result.id());
  }

  @Test
  void update_totalExceeds1_throwsBadRequest() {
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.existsByIdAndGroupCourseId(ID, GROUP_COURSE_ID)).thenReturn(true);
    when(repository.findByGroupCourseId(GROUP_COURSE_ID))
        .thenReturn(
            List.of(
                entity(ID, new BigDecimal("0.5000")), entity(OTHER_ID, new BigDecimal("0.3000"))));
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.8000"), GROUP_COURSE_ID);

    assertThrows(BadRequestException.class, () -> service.update(GROUP_COURSE_ID, ID, domain));
  }

  @Test
  void update_missingExam_throwsNotFound() {
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.of(groupCourse()));
    when(repository.existsByIdAndGroupCourseId(ID, GROUP_COURSE_ID)).thenReturn(false);
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.5000"), GROUP_COURSE_ID);

    assertThrows(NotFoundException.class, () -> service.update(GROUP_COURSE_ID, ID, domain));
  }

  @Test
  void update_missingGroupCourse_throwsNotFound() {
    when(groupCourseRepository.findById(GROUP_COURSE_ID)).thenReturn(Optional.empty());
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.5000"), GROUP_COURSE_ID);

    assertThrows(NotFoundException.class, () -> service.update(GROUP_COURSE_ID, ID, domain));
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(repository.existsByIdAndGroupCourseId(ID, GROUP_COURSE_ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.delete(GROUP_COURSE_ID, ID));
  }

  @Test
  void delete_existing_deletes() {
    when(repository.existsByIdAndGroupCourseId(ID, GROUP_COURSE_ID)).thenReturn(true);

    service.delete(GROUP_COURSE_ID, ID);

    verify(repository).deleteById(ID);
  }

  private JGroupCourse groupCourse() {
    var g = new JGroupCourse();
    g.setId(GROUP_COURSE_ID);
    return g;
  }

  private JExam entity(UUID id, BigDecimal coefficient) {
    var e = new JExam();
    e.setId(id);
    e.setExamDate(EXAM_DATE);
    e.setCoefficient(coefficient);
    e.setGroupCourse(groupCourse());
    return e;
  }
}
