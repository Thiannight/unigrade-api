package com.unigrade.api.repository.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "student_group")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(length = 2, nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private String reference;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promotion_id", nullable = false)
  private JPromotion promotion;

  @OneToMany(mappedBy = "group")
  private List<JMembership> memberships;

  @ManyToMany(mappedBy = "groups")
  private List<JCourse> courses;
}
