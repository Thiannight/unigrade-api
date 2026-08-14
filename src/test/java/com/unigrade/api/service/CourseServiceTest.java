package com.unigrade.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
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

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private CourseRepository repository;
  private final CourseMapper mapper = new CourseMapper();
  private CourseService service;

  @BeforeEach
  void setUp() {
    service = new CourseService(repository, mapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(repository.findAll()).thenReturn(List.of(entity()));

    List<Course> result = service.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).reference()).isEqualTo("CS101");
  }

  @Test
  void findById_existing_returnsMapped() throws NotFoundException {
    when(repository.findById(ID)).thenReturn(Optional.of(entity()));

    Course result = service.findById(ID);

    assertThat(result.id()).isEqualTo(ID);
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findById(ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void create_saves() throws ConflictException {
    var domain = new Course(null, "CS101", "Intro to CS", (short) 6);
    when(repository.save(any())).thenReturn(entity());

    Course result = service.create(domain);

    assertThat(result.reference()).isEqualTo("CS101");
    verify(repository).save(any());
  }

  @Test
  void create_duplicateConstraint_throwsConflict() {
    var domain = new Course(null, "CS101", "Intro to CS", (short) 6);
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThatThrownBy(() -> service.create(domain)).isInstanceOf(ConflictException.class);
  }

  @Test
  void update_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);
    var domain = new Course(ID, "CS101", "Intro to CS", (short) 6);

    assertThatThrownBy(() -> service.update(ID, domain)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void update_existing_saves() throws Exception {
    when(repository.existsById(ID)).thenReturn(true);
    when(repository.save(any())).thenReturn(entity());
    var domain = new Course(ID, "CS101", "Intro to CS", (short) 6);

    Course result = service.update(ID, domain);

    assertThat(result.id()).isEqualTo(ID);
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);

    assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void delete_existing_deletes() throws NotFoundException {
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
