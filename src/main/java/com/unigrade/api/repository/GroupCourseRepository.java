package com.unigrade.api.repository;

import com.unigrade.api.repository.model.JGroupCourse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupCourseRepository extends JpaRepository<JGroupCourse, UUID> {

  Optional<JGroupCourse> findByGroupIdAndCourseIdAndEndDateIsNull(UUID groupId, UUID courseId);

  List<JGroupCourse> findAllByGroupIdAndEndDateIsNull(UUID groupId);
}
