package com.unigrade.api.service;

import com.unigrade.api.exception.BadRequestException;
import com.unigrade.api.exception.ForbiddenException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.GradeMapper;
import com.unigrade.api.model.Grade;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.dto.GradeRequest;
import com.unigrade.api.repository.ExamRepository;
import com.unigrade.api.repository.GradeRepository;
import com.unigrade.api.repository.GroupCourseRepository;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.TeacherCourseRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JExam;
import com.unigrade.api.repository.model.JGrade;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JUser;
import com.unigrade.api.security.SecurityUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {

  private final GradeRepository repository;
  private final GroupCourseRepository groupCourseRepository;
  private final ExamRepository examRepository;
  private final UserRepository userRepository;
  private final MembershipRepository membershipRepository;
  private final TeacherCourseRepository teacherCourseRepository;
  private final GradeMapper mapper;

  public List<Grade> findByExam(UUID groupId, UUID courseId, UUID examId, String studentId) {
    JGroupCourse assignment = resolveActiveAssignment(groupId, courseId);
    JExam exam = resolveExam(assignment, examId);

    String effectiveStudentId = restrictToAllowedStudent(courseId, studentId);

    List<JGrade> grades = repository.findByExamIdOrderByGradeDateAsc(exam.getId());
    if (effectiveStudentId != null) {
      grades =
          grades.stream().filter(g -> g.getStudent().getId().equals(effectiveStudentId)).toList();
    }
    return grades.stream().map(mapper::toDomain).toList();
  }

  @Transactional
  public Grade grade(UUID groupId, UUID courseId, UUID examId, GradeRequest request) {
    requireCanGrade(courseId);

    JGroupCourse assignment = resolveActiveAssignment(groupId, courseId);
    JExam exam = resolveExam(assignment, examId);
    JUser student = resolveStudent(request.studentId());
    checkMembershipAtExamDate(assignment, student.getId(), exam.getExamDate());
    var grade =
        new Grade(
            null,
            exam.getId(),
            request.score(),
            request.gradeDate(),
            request.reason(),
            student.getId());
    return mapper.toDomain(repository.save(mapper.toEntity(grade, student, exam)));
  }

  private String restrictToAllowedStudent(UUID courseId, String requestedStudentId) {
    JUser current = SecurityUtils.currentUser();
    if (current.getRole() == Role.STUDENT) {
      if (requestedStudentId != null && !requestedStudentId.equals(current.getId())) {
        throw new ForbiddenException("Students can only view their own grades");
      }
      return current.getId();
    }
    if (current.getRole() == Role.TEACHER) {
      requireTeacherAssignedToCourse(current.getId(), courseId);
      return requestedStudentId;
    }
    return requestedStudentId;
  }

  private void requireCanGrade(UUID courseId) {
    JUser current = SecurityUtils.currentUser();
    if (current.getRole() == Role.STUDENT) {
      throw new ForbiddenException("Students cannot grade exams");
    }
    if (current.getRole() == Role.TEACHER) {
      requireTeacherAssignedToCourse(current.getId(), courseId);
    }
  }

  private void requireTeacherAssignedToCourse(String teacherId, UUID courseId) {
    if (!teacherCourseRepository.existsByCourseIdAndTeacherId(courseId, teacherId)) {
      throw new ForbiddenException("You are not assigned to this course");
    }
  }

  private JGroupCourse resolveActiveAssignment(UUID groupId, UUID courseId) {
    return groupCourseRepository
        .findByGroupIdAndCourseIdAndEndDateIsNull(groupId, courseId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No active course assignment for course " + courseId + " in group " + groupId));
  }

  private JExam resolveExam(JGroupCourse assignment, UUID examId) {
    return examRepository
        .findById(examId)
        .filter(exam -> assignment.getId().equals(exam.getGroupCourse().getId()))
        .orElseThrow(() -> new NotFoundException("Exam not found: " + examId));
  }

  private JUser resolveStudent(String studentId) {
    JUser student =
        userRepository
            .findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
    if (student.getRole() != Role.STUDENT) {
      throw new BadRequestException("User is not a student");
    }
    if (!student.getIsActive()) {
      throw new BadRequestException("Student is inactive");
    }
    return student;
  }

  private void checkMembershipAtExamDate(
      JGroupCourse assignment, String studentId, Instant examDate) {
    LocalDate date = examDate.atZone(ZoneId.systemDefault()).toLocalDate();
    if (!membershipRepository.existsByGroupIdAndStudentIdAt(
        assignment.getGroup().getId(), studentId, date)) {
      throw new BadRequestException(
          "Student " + studentId + " is not a member of this group at the exam date");
    }
  }
}
