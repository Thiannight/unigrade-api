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
@Table(name = "course")
@Getter
@Setter
public class Course {

  @Id
  @Column(length = 50)
  private String id;

  @Column(length = 10, nullable = false)
  private String ref;

  @Column(length = 50, nullable = false)
  private String title;

  @Column(nullable = false)
  private Short credits;
}
