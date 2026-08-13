package com.unigrade.api.mapper;

import com.unigrade.api.model.GroupCourse;
import com.unigrade.api.repository.model.JCourse;
import com.unigrade.api.repository.model.JGroupCourse;
import com.unigrade.api.repository.model.JStudentGroup;
import org.springframework.stereotype.Component;

@Component
public class GroupCourseMapper {

  public GroupCourse toDomain(JGroupCourse entity) {
    return new GroupCourse(entity.getId(), entity.getCourse().getId(), entity.getGroup().getId());
  }

  public JGroupCourse toEntity(GroupCourse domain, JCourse course, JStudentGroup group) {
    var entity = new JGroupCourse();
    entity.setId(domain.id());
    entity.setCourse(course);
    entity.setGroup(group);
    return entity;
  }
}
