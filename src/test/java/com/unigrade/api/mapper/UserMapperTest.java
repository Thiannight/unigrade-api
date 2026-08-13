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
    var entity = new JUser();
    entity.setId("STD00001");
    entity.setFirstName("Ada");
    entity.setLastName("Lovelace");
    entity.setBirthDate(LocalDate.of(2000, 1, 1));
    entity.setEmail("ada@unigrade.com");
    entity.setPassword("hashed-password");
    entity.setIsActive(true);
    entity.setRole(Role.STUDENT);

    User result = userMapper.toDomain(entity);

    assertEquals(entity.getId(), result.id());
    assertEquals(entity.getFirstName(), result.firstName());
    assertEquals(entity.getLastName(), result.lastName());
    assertEquals(entity.getBirthDate(), result.birthDate());
    assertEquals(entity.getEmail(), result.email());
    assertEquals(entity.getPassword(), result.password());
    assertEquals(entity.getIsActive(), result.isActive());
    assertEquals(Role.STUDENT, result.role());
  }

  @Test
  void toEntity_mapsRecordToEntity() {
    var domain =
        new User(
            "STD00001",
            "Ada",
            "Lovelace",
            LocalDate.of(2000, 1, 1),
            "ada@unigrade.com",
            "hashed-password",
            true,
            Role.STUDENT);

    JUser result = userMapper.toEntity(domain);

    assertEquals(domain.id(), result.getId());
    assertEquals(domain.firstName(), result.getFirstName());
    assertEquals(domain.lastName(), result.getLastName());
    assertEquals(domain.birthDate(), result.getBirthDate());
    assertEquals(domain.email(), result.getEmail());
    assertEquals(domain.password(), result.getPassword());
    assertEquals(domain.isActive(), result.getIsActive());
    assertEquals(Role.STUDENT, result.getRole());
  }
}
