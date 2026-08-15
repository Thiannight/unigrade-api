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
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .build();
  }

  public JGroupCourse toEntity(GroupCourse domain, JCourse course, JStudentGroup group) {
    return JGroupCourse.builder()
        .id(domain.id())
        .course(course)
        .group(group)
        .startDate(domain.startDate())
        .endDate(domain.endDate())
        .build();
  }
}
