package com.unigrade.api.mapper;

import com.unigrade.api.model.GroupCourse;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JStudentGroup;
import org.springframework.stereotype.Component;

@Component
public class GroupCourseMapper {

  public GroupCourse toDomain(JGroupCourse entity) {
    return GroupCourse.builder()
        .id(entity.getId())
        .courseId(entity.getCourse().getId())
        .groupId(entity.getGroup().getId())
        .schoolYear(entity.getSchoolYear())
        .build();
  }

  public JGroupCourse toEntity(GroupCourse domain, JCourse course, JStudentGroup group) {
    return JGroupCourse.builder()
        .id(domain.id())
        .course(course)
        .group(group)
        .schoolYear(domain.schoolYear())
        .build();
  }
}
