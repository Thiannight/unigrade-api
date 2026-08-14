package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.ExamMapper;
import com.unigrade.api.model.Exam;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JExam;
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
  private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final Instant EXAM_DATE = Instant.parse("2026-01-01T10:00:00Z");

  @Mock
  private ExamRepository repository;
  @Mock
  private CourseRepository courseRepository;
  private final ExamMapper mapper = new ExamMapper();
  private ExamService service;

  @BeforeEach
  void setUp() {
    service = new ExamService(repository, courseRepository, mapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity())));

    List<Exam> result = service.findAll(0, 10);

    assertEquals(1, result.size());
    assertEquals(COURSE_ID, result.get(0).courseId());
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findById(ID)).thenReturn(Optional.of(entity()));

    Exam result = service.findById(ID);

    assertEquals(ID, result.id());
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findById(ID));

    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void create_existingCourse_saves() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("0.5000"), COURSE_ID);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.save(any())).thenReturn(entity());

    Exam result = service.create(domain);

    assertEquals(COURSE_ID, result.courseId());
    verify(repository).save(any());
  }

  @Test
  void create_missingCourse_throwsNotFound() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("0.5000"), COURSE_ID);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(domain));
  }

  @Test
  void update_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.5000"), COURSE_ID);

    assertThrows(NotFoundException.class, () -> service.update(ID, domain));
  }

  @Test
  void update_missingCourse_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(true);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.5000"), COURSE_ID);

    assertThrows(NotFoundException.class, () -> service.update(ID, domain));
  }

  @Test
  void update_existing_saves() {
    when(repository.existsById(ID)).thenReturn(true);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.save(any())).thenReturn(entity());
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("0.5000"), COURSE_ID);

    Exam result = service.update(ID, domain);

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

  private JCourse course() {
    var c = new JCourse();
    c.setId(COURSE_ID);
    return c;
  }

  private JExam entity() {
    var e = new JExam();
    e.setId(ID);
    e.setExamDate(EXAM_DATE);
    e.setCoefficient(new BigDecimal("0.5000"));
    e.setCourse(course());
    return e;
  }
}