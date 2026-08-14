package com.unigrade.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.unigrade.api.exception.ConflictException;
import com.unigrade.api.exception.NotFoundException;
import com.unigrade.api.mapper.UserMapper;
import com.unigrade.api.model.Role;
import com.unigrade.api.model.User;
import com.unigrade.api.repository.UserRepository;
import com.unigrade.api.repository.model.JUser;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String ID = "STD00001";

  @Mock private UserRepository repository;
  private final UserMapper mapper = new UserMapper();
  private UserService service;

  @BeforeEach
  void setUp() {
    service = new UserService(repository, mapper);
  }

  @Test
  void findAll_returnsMappedList() {
    when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity())));

    List<User> result = service.findAll(0, 10);

    assertEquals(1, result.size());
    assertEquals(ID, result.get(0).id());
  }

  @Test
  void findById_existing_returnsMapped() {
    when(repository.findById(ID)).thenReturn(Optional.of(entity()));

    User result = service.findById(ID);

    assertEquals(ID, result.id());
  }

  @Test
  void findById_missing_throwsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findById(ID));

    assertTrue(exception.getMessage().contains("not found"));
  }

  @Test
  void create_generatesStudentIdAndSaves() {
    var domain = domain(null);
    when(repository.existsByEmail("ada@unigrade.com")).thenReturn(false);
    when(repository.findFirstByIdStartingWithOrderByIdDesc(anyString()))
        .thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    User result = service.create(domain);

    String expectedPrefix = "STD" + (Year.now().getValue() % 100);
    assertTrue(result.id().startsWith(expectedPrefix));
    assertTrue(result.id().endsWith("001"));
    verify(repository).save(any());
  }

  @Test
  void create_duplicateEmail_throwsConflict() {
    var domain = domain(null);
    when(repository.existsByEmail("ada@unigrade.com")).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.create(domain));
  }

  @Test
  void update_existing_savesWithPathId() {
    when(repository.existsById(ID)).thenReturn(true);
    when(repository.existsByEmailAndIdNot("ada@unigrade.com", ID)).thenReturn(false);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    User result = service.update(ID, domain(ID));

    assertEquals(ID, result.id());
    verify(repository).save(any());
  }

  @Test
  void update_changedEmailTaken_throwsConflict() {
    when(repository.existsById(ID)).thenReturn(true);
    when(repository.existsByEmailAndIdNot("other@unigrade.com", ID)).thenReturn(true);
    var domain =
        new User(
            ID,
            "Ada",
            "Lovelace",
            LocalDate.of(2000, 1, 1),
            "other@unigrade.com",
            "hashed-password",
            true,
            Role.STUDENT);

    assertThrows(ConflictException.class, () -> service.update(ID, domain));
  }

  @Test
  void update_missing_throwsNotFound() {
    when(repository.existsById(ID)).thenReturn(false);

    assertThrows(NotFoundException.class, () -> service.update(ID, domain(ID)));
  }

  @Test
  void delete_existing_deletes() {
    JUser user = entity();
    when(repository.findById(ID)).thenReturn(Optional.of(user));

    service.delete(ID);

    verify(repository).delete(user);
  }

  @Test
  void delete_missing_throwsNotFound() {
    when(repository.findById(ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> service.delete(ID));
  }

  private User domain(String id) {
    return new User(
        id,
        "Ada",
        "Lovelace",
        LocalDate.of(2000, 1, 1),
        "ada@unigrade.com",
        "hashed-password",
        true,
        Role.STUDENT);
  }

  private JUser entity() {
    var e = new JUser();
    e.setId(ID);
    e.setFirstName("Ada");
    e.setLastName("Lovelace");
    e.setBirthDate(LocalDate.of(2000, 1, 1));
    e.setEmail("ada@unigrade.com");
    e.setPassword("hashed-password");
    e.setIsActive(true);
    e.setRole(Role.STUDENT);
    return e;
  }
}
