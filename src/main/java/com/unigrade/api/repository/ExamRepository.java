package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JExam;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  Page<JExam> findAllByCourseId(UUID courseId, Pageable pageable);

  Page<JExam> findAllByCourseIdAndSchoolYear(UUID courseId, Short schoolYear, Pageable pageable);

  List<JExam> findByCourseIdAndSchoolYear(UUID courseId, Short schoolYear);

  Optional<JExam> findByIdAndCourseId(UUID id, UUID courseId);

  boolean existsByIdAndCourseId(UUID id, UUID courseId);
}
