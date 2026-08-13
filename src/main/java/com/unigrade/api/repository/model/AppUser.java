package com.unigrade.api.repository.model;

import com.unigrade.api.PojaGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@PojaGenerated
@Entity
@Table(name = "users")
@Getter
@Setter
public class AppUser {

  @Id
  @Column(length = 8)
  private String id;

  @Column(name = "first_name", length = 100, nullable = false)
  private String firstName;

  @Column(name = "last_name", length = 100, nullable = false)
  private String lastName;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(length = 50, nullable = false, unique = true)
  private String email;

  @Column(length = 255, nullable = false)
  private String password;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private Role role;
}
