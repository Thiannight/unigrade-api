package com.unigrade.api.repository.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.unigrade.api.model.Role;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JUser implements UserDetails {

  @Id
  @Column(length = 8)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String id;

  @Column(name = "first_name", length = 100, nullable = false)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(name = "birth_date", nullable = false)
  private LocalDate birthDate;

  @Column(length = 100, nullable = false, unique = true)
  private String email;

  @Column(length = 255, nullable = false)
  private String password;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, nullable = false)
  private Role role;

  @OneToMany(mappedBy = "student")
  private List<JMembership> memberships;

  @OneToMany(mappedBy = "student")
  private List<JGrade> grades;

  @OneToMany(mappedBy = "teacher")
  private List<JTeacherCourse> teacherCourses;

  @Transient
  @JsonIgnore
  public JStudentGroup getCurrentGroup() {
    return (memberships == null)
        ? null
        : this.memberships.stream()
            .filter(m -> m.getEndDate() == null)
            .map(JMembership::getGroup)
            .findFirst()
            .orElse(null);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getUsername() {
    return id;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(isActive);
  }
}
