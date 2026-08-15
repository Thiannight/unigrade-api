package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.TeacherCourseMapper;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.TeacherCourse;
import com.unigrade.api.model.dto.TeacherAssignmentRequest;
import com.unigrade.api.model.dto.TeacherPriorityRequest;
import com.unigrade.api.repository.CourseRepository;
import com.unigrade.api.repository.TeacherCourseRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JTeacherCourse;
import com.unigrade.api.repository.model.JUser;
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
class TeacherCourseServiceTest {

  private static final UUID COURSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ASSIGNMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String TEACHER_ID = "TCR00001";

  @Mock private TeacherCourseRepository repository;
  @Mock private CourseRepository courseRepository;
  @Mock private UserRepository userRepository;
  private final TeacherCourseMapper mapper = new TeacherCourseMapper();
  private TeacherCourseService service;

  @BeforeEach
  void setUp() {
    service = new TeacherCourseService(repository, courseRepository, userRepository, mapper);
  }

  @Test
  void findByCourse_returnsMappedOrderedList() {
    when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdOrderByPriorityAscTeacherIdAsc(COURSE_ID))
        .thenReturn(List.of(assignment((byte) 2)));

    List<TeacherCourse> result = service.findByCourse(COURSE_ID);

    assertEquals(1, result.size());
    assertEquals(TEACHER_ID, result.get(0).teacherId());
    assertEquals((byte) 2, result.get(0).priority());
  }

  @Test
  void findByCourse_missingCourse_throwsNotFound() {
    when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.findByCourse(COURSE_ID));

    assertTrue(exception.getMessage().contains("Course not found"));
  }

  @Test
  void assign_createsAssignment() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TeacherCourse result = service.assign(COURSE_ID, request);

    assertEquals(COURSE_ID, result.courseId());
    assertEquals(TEACHER_ID, result.teacherId());
    assertEquals((byte) 2, result.priority());
    verify(repository).save(any());
  }

  @Test
  void assign_missingCourse_throwsNotFound() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.assign(COURSE_ID, request));

    assertTrue(exception.getMessage().contains("Course not found"));
  }

  @Test
  void assign_missingTeacher_throwsNotFound() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.assign(COURSE_ID, request));

    assertTrue(exception.getMessage().contains("Teacher not found"));
  }

  @Test
  void assign_notTeacherRole_throwsBadRequest() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    var student = new JUser();
    student.setId(TEACHER_ID);
    student.setRole(Role.STUDENT);
    student.setIsActive(true);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(student));

    assertThrows(BadRequestException.class, () -> service.assign(COURSE_ID, request));
  }

  @Test
  void assign_inactiveTeacher_throwsBadRequest() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    var inactive = teacher();
    inactive.setIsActive(false);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(inactive));

    assertThrows(BadRequestException.class, () -> service.assign(COURSE_ID, request));
  }

  @Test
  void assign_duplicate_throwsConflict() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
    when(repository.existsByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID)).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.assign(COURSE_ID, request));
  }

  @Test
  void assign_duplicateConstraintRace_throwsConflict() {
    var request = new TeacherAssignmentRequest(TEACHER_ID, (byte) 2);
    when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course()));
    when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher()));
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(ConflictException.class, () -> service.assign(COURSE_ID, request));
  }

  @Test
  void updatePriority_updates() {
    var request = new TeacherPriorityRequest((byte) 4);
    when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(Optional.of(assignment((byte) 2)));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TeacherCourse result = service.updatePriority(COURSE_ID, TEACHER_ID, request);

    assertEquals((byte) 4, result.priority());
  }

  @Test
  void updatePriority_missingAssignment_throwsNotFound() {
    var request = new TeacherPriorityRequest((byte) 4);
    when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> service.updatePriority(COURSE_ID, TEACHER_ID, request));

    assertTrue(exception.getMessage().contains("not assigned"));
  }

  @Test
  void updatePriority_missingCourse_throwsNotFound() {
    var request = new TeacherPriorityRequest((byte) 4);
    when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> service.updatePriority(COURSE_ID, TEACHER_ID, request));

    assertTrue(exception.getMessage().contains("Course not found"));
  }

  @Test
  void remove_deletes() {
    when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID))
        .thenReturn(Optional.of(assignment((byte) 2)));

    service.remove(COURSE_ID, TEACHER_ID);

    verify(repository).delete(any(JTeacherCourse.class));
  }

  @Test
  void remove_missingAssignment_throwsNotFound() {
    when(courseRepository.existsById(COURSE_ID)).thenReturn(true);
    when(repository.findByCourseIdAndTeacherId(COURSE_ID, TEACHER_ID)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.remove(COURSE_ID, TEACHER_ID));

    assertTrue(exception.getMessage().contains("not assigned"));
  }

  @Test
  void remove_missingCourse_throwsNotFound() {
    when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> service.remove(COURSE_ID, TEACHER_ID));

    assertTrue(exception.getMessage().contains("Course not found"));
  }

  private JCourse course() {
    var course = new JCourse();
    course.setId(COURSE_ID);
    return course;
  }

  private JUser teacher() {
    var teacher = new JUser();
    teacher.setId(TEACHER_ID);
    teacher.setRole(Role.TEACHER);
    teacher.setIsActive(true);
    return teacher;
  }

  private JTeacherCourse assignment(Byte priority) {
    return JTeacherCourse.builder()
        .id(ASSIGNMENT_ID)
        .course(course())
        .teacher(teacher())
        .priority(priority)
        .build();
  }
}
