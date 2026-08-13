package com.unigrade.api.mapper;

import com.unigrade.api.model.Membership;
import com.unigrade.api.repository.model.JGroup;
import com.unigrade.api.repository.model.JMembership;
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

  public JMembership toEntity(Membership domain, JGroup group, JUser student) {
    return JMembership.builder()
        .id(domain.id())
        .group(group)
        .student(student)
        .startDate(domain.startDate())
        .endDate(domain.endDate())
        .build();
  }
}
