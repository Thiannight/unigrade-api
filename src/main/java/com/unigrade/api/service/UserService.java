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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository repository;
  private final UserMapper mapper;
  private final PasswordEncoder passwordEncoder;

  public List<User> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50)))
        .map(mapper::toDomain)
        .toList();
  }

  public User findById(String id) {
    return repository.findById(id).map(mapper::toDomain).orElseThrow(this::userNotFound);
  }

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
            passwordEncoder.encode(user.password()),
            user.isActive(),
            user.role());
    return mapper.toDomain(raceAwareSave(mapper.toEntity(withId)));
  }

  public User update(String id, User user) {
    JUser existing = repository.findById(id).orElseThrow(this::userNotFound);

    String email = user.email().toLowerCase();
    if (repository.existsByEmailAndIdNot(email, id)) {
      throw emailAlreadyExists();
    }

    String password =
        user.password() == null || user.password().isBlank()
            ? existing.getPassword()
            : passwordEncoder.encode(user.password());

    JUser toSave =
        JUser.builder()
            .id(id)
            .firstName(user.firstName())
            .lastName(user.lastName())
            .birthDate(user.birthDate())
            .email(email)
            .password(password)
            .isActive(user.isActive())
            .role(user.role())
            .build();
    return mapper.toDomain(raceAwareSave(toSave));
  }

  public void delete(String id) {
    repository.delete(repository.findById(id).orElseThrow(this::userNotFound));
  }

  public void deactivate(String id) {
    JUser user = repository.findById(id).orElseThrow(this::userNotFound);
    user.setIsActive(false);
    repository.save(user);
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

  private JUser raceAwareSave(JUser user) {
    try {
      return repository.save(user);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("User or email already exists");
    }
  }

  private NotFoundException userNotFound() {
    return new NotFoundException("User not found");
  }

  private ConflictException emailAlreadyExists() {
    return new ConflictException("Email already exists");
  }
}
