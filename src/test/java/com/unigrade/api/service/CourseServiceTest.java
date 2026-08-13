package com.unigrade.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.mapper.CourseMapper;
import com.unigrade.api.model.Course;
import com.unigrade.api.repository.CourseRepository;
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
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private CourseRepository courseRepository;
  private final CourseMapper courseMapper = new CourseMapper();
  private CourseService courseService;

  @BeforeEach
  void setUp() {
    courseService = new CourseService(courseRepository, courseMapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(courseRepository.findAll()).thenReturn(List.of(entity()));

    List<Course> result = courseService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).reference()).isEqualTo("CS101");
  }

  @Test
  void findById_existing_returnsMapped() {
    when(courseRepository.findById(ID)).thenReturn(Optional.of(entity()));

    Course result = courseService.findById(ID);

    assertThat(result.id()).isEqualTo(ID);
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(courseRepository.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.findById(ID))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void create_saves() {
    var domain = new Course(null, "CS101", "Intro to CS", (short) 6);
    when(courseRepository.save(any())).thenReturn(entity());

    Course result = courseService.create(domain);

    assertThat(result.reference()).isEqualTo("CS101");
    verify(courseRepository).save(any());
  }

  @Test
  void create_duplicateConstraint_throwsConflict() {
    var domain = new Course(null, "CS101", "Intro to CS", (short) 6);
    when(courseRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThatThrownBy(() -> courseService.create(domain))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");
  }

  @Test
  void update_missing_throwsNotFound() {
    when(courseRepository.existsById(ID)).thenReturn(false);
    var domain = new Course(ID, "CS101", "Intro to CS", (short) 6);

    assertThatThrownBy(() -> courseService.update(ID, domain))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void update_existing_saves() {
    when(courseRepository.existsById(ID)).thenReturn(true);
    when(courseRepository.save(any())).thenReturn(entity());
    var domain = new Course(ID, "CS101", "Intro to CS", (short) 6);

    Course result = courseService.update(ID, domain);

    assertThat(result.id()).isEqualTo(ID);
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(courseRepository.existsById(ID)).thenReturn(false);

    assertThatThrownBy(() -> courseService.delete(ID))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  @Test
  void delete_existing_deletes() {
    when(courseRepository.existsById(ID)).thenReturn(true);

    courseService.delete(ID);

    verify(courseRepository).deleteById(ID);
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
