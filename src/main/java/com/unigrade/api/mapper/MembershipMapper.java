package com.unigrade.api.mapper;

import com.unigrade.api.model.Membership;
import com.unigrade.api.repository.model.JMembership;
import com.unigrade.api.repository.model.JStudentGroup;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class MembershipMapper {

  public Membership toDomain(JMembership entity) {
    return new Membership(
        entity.getId(),
        entity.getGroup().getId(),
        entity.getStudent().getId(),
        entity.getStartDate(),
        entity.getEndDate());
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
