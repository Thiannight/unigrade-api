package com.unigrade.api.repository.model;

import com.unigrade.api.model.Role;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JUser {

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

  @Column(length = 100, nullable = false)
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

  @ManyToMany(mappedBy = "teachers")
  private List<JCourse> taughtCourses;
}
