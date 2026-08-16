package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JGroupCourse;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCourseRepository extends JpaRepository<JGroupCourse, UUID> {

  Optional<JGroupCourse> findByGroupIdAndCourseIdAndEndDateIsNull(UUID groupId, UUID courseId);

  List<JGroupCourse> findAllByGroupIdAndEndDateIsNull(UUID groupId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT gc FROM JGroupCourse gc WHERE gc.group.id = :groupId AND gc.course.id = :courseId AND"
          + " gc.endDate IS NULL")
  Optional<JGroupCourse> findActiveForUpdate(
      @Param("groupId") UUID groupId, @Param("courseId") UUID courseId);
}
