package com.unigrade.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.unigrade.api.model.Role;
import com.unigrade.api.model.User;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserMapperTest {
  private final UserMapper userMapper = new UserMapper();

  @Test
  void toDomain_mapsEntityToRecord() {
    JUser entity =
        JUser.builder()
            .id("STD00001")
            .firstName("John")
            .lastName("Doe")
            .birthDate(LocalDate.of(2000, 1, 1))
            .email("john.doe@example.com")
            .password("secret")
            .isActive(true)
            .role(Role.STUDENT)
            .build();

    User result = userMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getFirstName(), result.firstName());
    assertEquals(entity.getLastName(), result.lastName());
    assertEquals(entity.getBirthDate(), result.birthDate());
    assertEquals(entity.getEmail(), result.email());
    assertEquals(entity.getPassword(), result.password());
    assertEquals(entity.getIsActive(), result.isActive());
    assertEquals(entity.getRole(), result.role());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    User domain =
        User.builder()
            .id("STD00001")
            .firstName("John")
            .lastName("Doe")
            .birthDate(LocalDate.of(2000, 1, 1))
            .email("john.doe@example.com")
            .password("secret")
            .isActive(true)
            .role(Role.STUDENT)
            .build();

    JUser result = userMapper.toEntity(domain);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.firstName(), result.getFirstName());
    assertEquals(domain.lastName(), result.getLastName());
    assertEquals(domain.birthDate(), result.getBirthDate());
    assertEquals(domain.email(), result.getEmail());
    assertEquals(domain.password(), result.getPassword());
    assertEquals(domain.isActive(), result.getIsActive());
    assertEquals(domain.role(), result.getRole());
  }
}
