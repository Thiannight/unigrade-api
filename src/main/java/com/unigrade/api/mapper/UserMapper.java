package com.unigrade.api.mapper;

import com.unigrade.api.model.Role;
import com.unigrade.api.model.User;
import com.unigrade.api.repository.model.JRole;
import com.unigrade.api.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User toDomain(JUser entity) {
    return User.builder()
        .id(entity.getId())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .birthDate(entity.getBirthDate())
        .email(entity.getEmail())
        .password(entity.getPassword())
        .isActive(entity.getIsActive())
        .role(Role.valueOf(entity.getRole().name()))
        .build();
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
