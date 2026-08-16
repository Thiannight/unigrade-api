package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JExam;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  boolean existsByGroupCourseId(UUID groupCourseId);

  List<JExam> findByGroupCourseIdOrderByExamDateAsc(UUID groupCourseId);
}
