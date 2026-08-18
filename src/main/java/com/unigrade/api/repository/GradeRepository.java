package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JGrade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {

  List<JGrade> findByExamIdOrderByGradeDateAsc(UUID examId);

  Optional<JGrade> findTopByExamIdAndStudentIdOrderByGradeDateDesc(UUID examId, String studentId);

  @Query(
      "SELECT g FROM JGrade g WHERE g.student.id = :studentId AND g.exam.id IN :examIds ORDER BY"
          + " g.gradeDate DESC")
  List<JGrade> findByStudentIdAndExamIdsOrderByGradeDateDesc(
      @Param("studentId") String studentId, @Param("examIds") List<UUID> examIds);

  boolean existsByExamId(UUID examId);
}
