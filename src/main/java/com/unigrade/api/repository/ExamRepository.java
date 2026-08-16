package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JExam;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  boolean existsByGroupCourseId(UUID groupCourseId);

  List<JExam> findByGroupCourseIdOrderByExamDateAsc(UUID groupCourseId);

  Optional<JExam> findByIdAndGroupCourseId(UUID id, UUID groupCourseId);

  @Query(
      "SELECT COALESCE(SUM(e.coefficient), 0) FROM JExam e WHERE e.groupCourse.id = :groupCourseId")
  BigDecimal sumCoefficientByGroupCourseId(@Param("groupCourseId") UUID groupCourseId);

  @Query(
      "SELECT COALESCE(SUM(e.coefficient), 0) FROM JExam e WHERE e.groupCourse.id = :groupCourseId"
          + " AND e.id <> :excludedExamId")
  BigDecimal sumCoefficientByGroupCourseIdExcluding(
      @Param("groupCourseId") UUID groupCourseId, @Param("excludedExamId") UUID excludedExamId);
}
