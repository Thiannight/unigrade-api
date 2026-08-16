package com.unigrade.api.repository;

import com.unigrade.api.model.Semester;
import com.unigrade.api.repository.model.JGroupCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCourseRepository extends JpaRepository<JGroupCourse, UUID> {

  Optional<JGroupCourse> findByGroupIdAndCourseIdAndEndDateIsNull(UUID groupId, UUID courseId);

  List<JGroupCourse> findAllByGroupIdAndEndDateIsNull(UUID groupId);

  List<JGroupCourse> findAllByGroupId(UUID groupId);

  @Query(
      """
      SELECT COALESCE(SUM(gc.course.credits), 0) FROM JGroupCourse gc
      WHERE gc.group.id = :groupId
        AND gc.semester = :semester
        AND gc.endDate IS NULL
      """)
  Long sumCreditsByGroupIdAndSemester(
      @Param("groupId") UUID groupId, @Param("semester") Semester semester);
}
