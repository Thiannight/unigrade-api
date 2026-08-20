package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.UserMapper;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.User;
import com.unigrade.api.repository.MembershipRepository;
import com.unigrade.api.repository.TeacherCourseRepository;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String STUDENT_ID = "STD00001";

  @Mock private UserRepository repository;
  @Mock private MembershipRepository membershipRepository;
  @Mock private TeacherCourseRepository teacherCourseRepository;
  @Mock private PasswordEncoder passwordEncoder;
  private final UserMapper mapper = new UserMapper();
  private UserService service;

  @BeforeEach
  void setUp() {
    service =
        new UserService(
            repository, membershipRepository, teacherCourseRepository, mapper, passwordEncoder);
  }

  @Test
  void create_hashesPassword() {
    var domain = domainUser("ada@unigrade.com", "clear-text-password");
    when(repository.existsByEmail("ada@unigrade.com")).thenReturn(false);
    when(passwordEncoder.encode("clear-text-password")).thenReturn("$2a$10$hashed");
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    User result = service.create(domain);

    assertEquals("$2a$10$hashed", result.password());
    assertNotEquals("clear-text-password", result.password());
  }

  @Test
  void create_duplicateEmail_throwsConflict() {
    var domain = domainUser("dup@unigrade.com", "pw");
    when(repository.existsByEmail("dup@unigrade.com")).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.create(domain));
  }

  @Test
  void create_lowercasesEmail() {
    var domain = domainUser("Mixed.Case@Unigrade.com", "pw");
    when(repository.existsByEmail("mixed.case@unigrade.com")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("hashed");
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    User result = service.create(domain);

    assertEquals("mixed.case@unigrade.com", result.email());
  }

  @Test
  void update_blankPassword_keepsExistingHash() {
    JUser existing = entity("ada@unigrade.com", "$2a$10$existing-hash");
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.of(existing));
    when(repository.existsByEmailAndIdNot("ada@unigrade.com", STUDENT_ID)).thenReturn(false);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var update =
        new User(
            STUDENT_ID,
            "Ada",
            "Lovelace",
            LocalDate.of(2000, 1, 1),
            "ada@unigrade.com",
            "",
            true,
            Role.STUDENT,
            null);

    User result = service.update(STUDENT_ID, update);

    assertEquals("$2a$10$existing-hash", result.password());
  }

  @Test
  void update_newPassword_reHashes() {
    JUser existing = entity("ada@unigrade.com", "$2a$10$existing-hash");
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.of(existing));
    when(repository.existsByEmailAndIdNot("ada@unigrade.com", STUDENT_ID)).thenReturn(false);
    when(passwordEncoder.encode("new-password")).thenReturn("$2a$10$new-hash");
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var update =
        new User(
            STUDENT_ID,
            "Ada",
            "Lovelace",
            LocalDate.of(2000, 1, 1),
            "ada@unigrade.com",
            "new-password",
            true,
            Role.STUDENT,
            null);

    User result = service.update(STUDENT_ID, update);

    assertEquals("$2a$10$new-hash", result.password());
  }

  @Test
  void update_missingUser_throwsNotFound() {
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    var update = domainUser("ada@unigrade.com", "pw");

    assertThrows(NotFoundException.class, () -> service.update(STUDENT_ID, update));
  }

  @Test
  void update_duplicateEmail_throwsConflict() {
    JUser existing = entity("ada@unigrade.com", "hash");
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.of(existing));
    when(repository.existsByEmailAndIdNot("taken@unigrade.com", STUDENT_ID)).thenReturn(true);

    var update =
        new User(
            STUDENT_ID,
            "Ada",
            "Lovelace",
            LocalDate.of(2000, 1, 1),
            "taken@unigrade.com",
            "pw",
            true,
            Role.STUDENT,
            null);

    assertThrows(ConflictException.class, () -> service.update(STUDENT_ID, update));
  }

  @Test
  void create_raceOnUniqueConstraint_throwsConflict() {
    var domain = domainUser("ada@unigrade.com", "pw");
    when(repository.existsByEmail("ada@unigrade.com")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("hashed");
    when(repository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThrows(ConflictException.class, () -> service.create(domain));
  }

  @Test
  void deactivate_setsInactive() {
    JUser existing = entity("ada@unigrade.com", "hash");
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.of(existing));

    service.deactivate(STUDENT_ID);

    ArgumentCaptor<JUser> captor = ArgumentCaptor.forClass(JUser.class);
    verify(repository).save(captor.capture());
    assertEquals(false, captor.getValue().getIsActive());
  }

  @Test
  void deactivate_missingUser_throwsNotFound() {
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.deactivate(STUDENT_ID));
  }

  @Test
  void delete_missingUser_throwsNotFound() {
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.delete(STUDENT_ID));
  }

  @Test
  void delete_existing_deletes() {
    JUser existing = entity("ada@unigrade.com", "hash");
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.of(existing));

    service.delete(STUDENT_ID);

    verify(repository).delete(existing);
  }

  @Test
  void loadUserByUsername_returnsEntity() {
    JUser existing = entity("ada@unigrade.com", "hash");
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.of(existing));

    assertSame(existing, service.loadUserByUsername(STUDENT_ID));
  }

  @Test
  void loadUserByUsername_missingUser_throwsUsernameNotFound() {
    when(repository.findById(STUDENT_ID)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(STUDENT_ID));
  }

  private User domainUser(String email, String password) {
    return new User(
        null,
        "Ada",
        "Lovelace",
        LocalDate.of(2000, 1, 1),
        email,
        password,
        true,
        Role.STUDENT,
        null);
  }

  private JUser entity(String email, String passwordHash) {
    var user = new JUser();
    user.setId(STUDENT_ID);
    user.setFirstName("Ada");
    user.setLastName("Lovelace");
    user.setBirthDate(LocalDate.of(2000, 1, 1));
    user.setEmail(email);
    user.setPassword(passwordHash);
    user.setIsActive(true);
    user.setRole(Role.STUDENT);
    return user;
  }
}
