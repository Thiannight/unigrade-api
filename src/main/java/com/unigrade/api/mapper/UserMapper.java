package com.unigrade.api.mapper;

import com.unigrade.api.model.Role;
import com.unigrade.api.model.User;
import com.unigrade.api.repository.model.JRole;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User toDomain(JUser entity) {
    return new User(
        entity.getId(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getBirthDate(),
        entity.getEmail(),
        entity.getPassword(),
        entity.getIsActive(),
        Role.valueOf(entity.getRole().name()));
  }

  public JUser toEntity(User domain) {
    var entity = new JUser();
    entity.setId(domain.id());
    entity.setFirstName(domain.firstName());
    entity.setLastName(domain.lastName());
    entity.setBirthDate(domain.birthDate());
    entity.setEmail(domain.email());
    entity.setPassword(domain.password());
    entity.setIsActive(domain.isActive());
    entity.setRole(JRole.valueOf(domain.role().name()));
    return entity;
  }
}
