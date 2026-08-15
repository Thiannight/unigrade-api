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

  Page<JExam> findAllByGroupCourseId(UUID groupCourseId, Pageable pageable);

  List<JExam> findByGroupCourseId(UUID groupCourseId);

  Optional<JExam> findByIdAndGroupCourseId(UUID id, UUID groupCourseId);

  boolean existsByIdAndGroupCourseId(UUID id, UUID groupCourseId);
}
