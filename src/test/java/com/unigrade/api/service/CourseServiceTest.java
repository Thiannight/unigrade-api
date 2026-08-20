package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.CourseMapper;
import com.unigrade.api.model.Course;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.model.JCourse;
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
class CourseServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private CourseRepository repository;
  @Mock private GroupCourseRepository groupCourseRepository;
  private final CourseMapper mapper = new CourseMapper();
  private CourseService service;

  @BeforeEach
  void setUp() {
    service = new CourseService(repository, groupCourseRepository, mapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity())));

    List<Course> result = service.findAll(1, 10);

    assertEquals(1, result.size());
    assertEquals("CS101", result.get(0).reference());
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findById(ID)).thenReturn(Optional.of(entity()));

    Course result = service.findById(ID);

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
    var domain = new Course(null, "CS101", "Intro to CS", (short) 6);
    when(repository.save(any())).thenReturn(entity());

    Course result = service.create(domain);

    assertEquals("CS101", result.reference());
    verify(repository).save(any());
  }

  @Test
  void create_duplicateConstraint_throwsConflict() {
    var domain = new Course(null, "CS101", "Intro to CS", (short) 6);
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(ConflictException.class, () -> service.create(domain));
  }

  @Test
  void update_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);
    var domain = new Course(ID, "CS101", "Intro to CS", (short) 6);

    assertThrows(NotFoundException.class, () -> service.update(ID, domain));
  }

  @Test
  void update_existing_saves() {
    when(repository.existsById(ID)).thenReturn(true);
    when(repository.save(any())).thenReturn(entity());
    var domain = new Course(ID, "CS101", "Intro to CS", (short) 6);

    Course result = service.update(ID, domain);

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

  private JCourse entity() {
    var e = new JCourse();
    e.setId(ID);
    e.setReference("CS101");
    e.setTitle("Intro to CS");
    e.setCredits((short) 6);
    return e;
  }
}
