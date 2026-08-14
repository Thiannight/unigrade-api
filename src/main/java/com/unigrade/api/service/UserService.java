package com.unigrade.api.service;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.UserMapper;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.User;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository repository;
  private final UserMapper mapper;

  public List<User> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)))
        .map(mapper::toDomain)
        .toList();
  }

  public User findById(String id) {
    return repository.findById(id).map(mapper::toDomain).orElseThrow(this::userNotFound);
  }

  // TODO: Encode the passwords when Spring Security will be integrated
  public User create(User user) {
    String email = user.email().toLowerCase();
    if (repository.existsByEmail(email)) {
      throw emailAlreadyExists();
    }

    User withId =
        new User(
            generateId(user.role()),
            user.firstName(),
            user.lastName(),
            user.birthDate(),
            email,
            user.password(),
            user.isActive(),
            user.role());
    return mapper.toDomain(repository.save(mapper.toEntity(withId)));
  }

  public User update(String id, User user) {
    if (!repository.existsById(id)) {
      throw userNotFound();
    }

    String email = user.email().toLowerCase();
    if (repository.existsByEmailAndIdNot(email, id)) {
      throw emailAlreadyExists();
    }

    JUser toSave =
        JUser.builder()
            .id(id)
            .firstName(user.firstName())
            .lastName(user.lastName())
            .birthDate(user.birthDate())
            .email(email)
            .password(user.password())
            .isActive(user.isActive())
            .role(user.role())
            .build();
    return mapper.toDomain(repository.save(toSave));
  }

  public void delete(String id) {
    repository.delete(repository.findById(id).orElseThrow(this::userNotFound));
  }

  private String generateId(Role role) {
    return switch (role) {
      case STUDENT -> generateStudentId();
      case TEACHER -> "TCR" + nextNumber("TCR", 5);
      case ADMIN -> "MGR" + nextNumber("MGR", 5);
    };
  }

  private String nextNumber(String prefix, int digits) {
    int last =
        repository
            .findFirstByIdStartingWithOrderByIdDesc(prefix)
            .map(entity -> Integer.parseInt(entity.getId().substring(prefix.length())))
            .orElse(0);
    return "0".repeat(digits - Integer.toString(last + 1).length()) + (last + 1);
  }

  private String generateStudentId() {
    String prefix = "STD" + (Year.now().getValue() % 100);
    return prefix + nextNumber(prefix, 3);
  }

  private NotFoundException userNotFound() {
    return new NotFoundException("User not found");
  }

  private ConflictException emailAlreadyExists() {
    return new ConflictException("Email already exists");
  }
}
