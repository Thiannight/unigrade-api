package com.unigrade.api.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {
  private String id;
  private String firstName;
  private String lastName;
  private LocalDate birthDate;
  private String email;
  private String password;
  private Boolean isActive;
  private Role role;
}
