package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JMembership;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<JMembership, UUID> {

  Optional<JMembership> findByGroupIdAndStudentIdAndEndDateIsNull(UUID groupId, String studentId);

  @Query(
      """
      SELECT m FROM JMembership m
      WHERE m.group.id = :groupId
        AND m.startDate <= :date
        AND (m.endDate IS NULL OR m.endDate > :date)
        AND (:includeInactive = true OR m.student.isActive = true)
      """)
  Page<JMembership> findMembersAt(
      @Param("groupId") UUID groupId,
      @Param("date") LocalDate date,
      @Param("includeInactive") boolean includeInactive,
      Pageable pageable);

  @Query(
      """
      SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
      FROM JMembership m
      WHERE m.group.id = :groupId
        AND m.student.id = :studentId
        AND m.startDate <= :date
        AND (m.endDate IS NULL OR m.endDate > :date)
      """)
  boolean existsByGroupIdAndStudentIdAt(
      @Param("groupId") UUID groupId,
      @Param("studentId") String studentId,
      @Param("date") LocalDate date);
}
