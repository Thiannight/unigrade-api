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
  private static final UUID OTHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID COURSE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final Instant EXAM_DATE = Instant.parse("2026-01-01T10:00:00Z");
  private static final short SCHOOL_YEAR = 2026;

  @Mock private ExamRepository repository;
  @Mock private CourseRepository courseRepository;
  private final ExamMapper mapper = new ExamMapper();
  private ExamService service;

  @BeforeEach
  void setUp() {
    service = new ExamService(repository, courseRepository, mapper);
  }

  @Test
  void findAll_noSchoolYearFilter_returnsMappedList() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.findAllByCourseId(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR))));

    List<Exam> result = service.findAll(COURSE_ID, null, 0, 10);

    assertEquals(1, result.size());
    assertEquals(COURSE_ID, result.get(0).courseId());
  }

  @Test
  void findAll_withSchoolYearFilter_returnsMappedList() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.findAllByCourseIdAndSchoolYear(any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR))));

    List<Exam> result = service.findAll(COURSE_ID, SCHOOL_YEAR, 0, 10);

    assertEquals(1, result.size());
    assertEquals(SCHOOL_YEAR, result.get(0).schoolYear());
  }

  @Test
  void findAll_missingCourse_throwsNotFound() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.findAll(COURSE_ID, null, 0, 10));
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findByIdAndCourseId(ID, COURSE_ID))
        .thenReturn(Optional.of(entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR)));

    Exam result = service.findById(COURSE_ID, ID);

    assertEquals(ID, result.id());
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findByIdAndCourseId(ID, COURSE_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.findById(COURSE_ID, ID));

    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void create_underTotal_saves() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("50.00"), COURSE_ID, SCHOOL_YEAR);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.findByCourseIdAndSchoolYear(COURSE_ID, SCHOOL_YEAR)).thenReturn(List.of());
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR));

    Exam result = service.create(COURSE_ID, domain);

    assertEquals(COURSE_ID, result.courseId());
    verify(repository).save(any());
  }

  @Test
  void create_missingCourse_throwsNotFound() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("50.00"), COURSE_ID, SCHOOL_YEAR);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.create(COURSE_ID, domain));
  }

  @Test
  void create_totalExceeds100ForSameYear_throwsBadRequest() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("60.00"), COURSE_ID, SCHOOL_YEAR);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.findByCourseIdAndSchoolYear(COURSE_ID, SCHOOL_YEAR))
        .thenReturn(List.of(entity(OTHER_ID, new BigDecimal("50.00"), SCHOOL_YEAR)));

    assertThrows(BadRequestException.class, () -> service.create(COURSE_ID, domain));
  }

  @Test
  void create_differentSchoolYear_doesNotCountTowardOtherYearTotal() {
    short nextYear = (short) (SCHOOL_YEAR + 2);
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("60.00"), COURSE_ID, nextYear);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    // 2026 already has 60% used up, but this exam is for 2028 — should not conflict
    when(repository.findByCourseIdAndSchoolYear(COURSE_ID, nextYear)).thenReturn(List.of());
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("60.00"), nextYear));

    Exam result = service.create(COURSE_ID, domain);

    assertEquals(nextYear, result.schoolYear());
  }

  @Test
  void create_totalExactly100_saves() {
    var domain = new Exam(null, EXAM_DATE, new BigDecimal("50.00"), COURSE_ID, SCHOOL_YEAR);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.findByCourseIdAndSchoolYear(COURSE_ID, SCHOOL_YEAR))
        .thenReturn(List.of(entity(OTHER_ID, new BigDecimal("50.00"), SCHOOL_YEAR)));
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR));

    Exam result = service.create(COURSE_ID, domain);

    assertEquals(ID, result.id());
  }

  @Test
  void update_excludesItselfFromTotal_saves() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.existsByIdAndCourseId(ID, COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdAndSchoolYear(COURSE_ID, SCHOOL_YEAR))
        .thenReturn(
            List.of(
                entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR),
                entity(OTHER_ID, new BigDecimal("30.00"), SCHOOL_YEAR)));
    when(repository.save(any())).thenReturn(entity(ID, new BigDecimal("70.00"), SCHOOL_YEAR));
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("70.00"), COURSE_ID, SCHOOL_YEAR);

    Exam result = service.update(COURSE_ID, ID, domain);

    assertEquals(ID, result.id());
  }

  @Test
  void update_totalExceeds100_throwsBadRequest() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.existsByIdAndCourseId(ID, COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdAndSchoolYear(COURSE_ID, SCHOOL_YEAR))
        .thenReturn(
            List.of(
                entity(ID, new BigDecimal("50.00"), SCHOOL_YEAR),
                entity(OTHER_ID, new BigDecimal("30.00"), SCHOOL_YEAR)));
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("80.00"), COURSE_ID, SCHOOL_YEAR);

    assertThrows(BadRequestException.class, () -> service.update(COURSE_ID, ID, domain));
  }

  @Test
  void update_missingExam_throwsNotFound() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(repository.existsByIdAndCourseId(ID, COURSE_ID)).thenReturn(false);
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("50.00"), COURSE_ID, SCHOOL_YEAR);

    assertThrows(NotFoundException.class, () -> service.update(COURSE_ID, ID, domain));
  }

  @Test
  void update_missingCourse_throwsNotFound() {
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());
    var domain = new Exam(ID, EXAM_DATE, new BigDecimal("50.00"), COURSE_ID, SCHOOL_YEAR);

    assertThrows(NotFoundException.class, () -> service.update(COURSE_ID, ID, domain));
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(repository.existsByIdAndCourseId(ID, COURSE_ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.delete(COURSE_ID, ID));
  }

  @Test
  void delete_existing_deletes() {
    when(repository.existsByIdAndCourseId(ID, COURSE_ID)).thenReturn(true);

    service.delete(COURSE_ID, ID);

    verify(repository).deleteById(ID);
  }

  private JCourse course() {
    var c = new JCourse();
    c.setId(COURSE_ID);
    return c;
  }

  private JExam entity(UUID id, BigDecimal coefficient, short schoolYear) {
    var e = new JExam();
    e.setId(id);
    e.setExamDate(EXAM_DATE);
    e.setCoefficient(coefficient);
    e.setSchoolYear(schoolYear);
    e.setCourse(course());
    return e;
  }
}
