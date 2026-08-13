package com.unigrade.api.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
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
}
