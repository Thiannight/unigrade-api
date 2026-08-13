package com.unigrade.api.repository.model;

import com.unigrade.api.PojaGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@PojaGenerated
@Entity
@Table(name = "promotion")
@Getter
@Setter
public class Promotion {

  @Id
  @Column(length = 50)
  private String id;

  @Column(length = 50, nullable = false)
  private String ref;

  @Column(name = "start_year", nullable = false)
  private Short startYear;

  @Column(name = "end_year", nullable = false)
  private Short endYear;
}
