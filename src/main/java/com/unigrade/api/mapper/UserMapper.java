package com.unigrade.api.mapper;

import com.unigrade.api.model.User;
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
        .role(entity.getRole())
        .build();
  }

  public JUser toEntity(User domain) {
    return JUser.builder()
        .id(domain.id())
        .firstName(domain.firstName())
        .lastName(domain.lastName())
        .birthDate(domain.birthDate())
        .email(domain.email())
        .password(domain.password())
        .isActive(domain.isActive())
        .role(domain.role())
        .build();
  }
}
