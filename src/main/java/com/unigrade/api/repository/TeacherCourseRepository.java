package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JTeacherCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherCourseRepository extends JpaRepository<JTeacherCourse, UUID> {

  List<JTeacherCourse> findByCourseIdOrderByPriorityAscTeacherIdAsc(UUID courseId);

  Optional<JTeacherCourse> findByCourseIdAndTeacherId(UUID courseId, String teacherId);

  boolean existsByCourseIdAndTeacherId(UUID courseId, String teacherId);

  boolean existsByTeacherId(String teacherId);
}
