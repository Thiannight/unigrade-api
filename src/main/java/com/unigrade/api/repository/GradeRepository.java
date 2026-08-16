package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JGrade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {

  List<JGrade> findByExamIdOrderByGradeDateAsc(UUID examId);

  boolean existsByExamId(UUID examId);
}
