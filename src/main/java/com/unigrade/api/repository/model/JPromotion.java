package com.unigrade.api.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "promotion",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = "reference"),
      @UniqueConstraint(columnNames = "start_year"),
      @UniqueConstraint(columnNames = "end_year")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JPromotion {

  @Id
  @Column(length = 36)
  private UUID id;

  @Column(length = 50, nullable = false)
  private String reference;

  @Column(name = "start_year", nullable = false)
  private Short startYear;

  @Column(name = "end_year", nullable = false)
  private Short endYear;

  @OneToMany(mappedBy = "promotion")
  private List<JStudentGroup> studentGroups;
}
