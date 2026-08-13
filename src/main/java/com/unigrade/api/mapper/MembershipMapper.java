package com.unigrade.api.mapper;

import com.unigrade.api.model.Membership;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper {

  public Membership toDomain(JMembership entity) {
    return Membership.builder()
        .id(entity.getId())
        .groupId(entity.getGroup().getId())
        .studentId(entity.getStudent().getId())
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .build();
  }

  public JMembership toEntity(Membership domain, JStudentGroup group, JUser student) {
    var entity = new JMembership();
    entity.setId(domain.id());
    entity.setGroup(group);
    entity.setStudent(student);
    entity.setStartDate(domain.startDate());
    entity.setEndDate(domain.endDate());
    return entity;
  }
}
