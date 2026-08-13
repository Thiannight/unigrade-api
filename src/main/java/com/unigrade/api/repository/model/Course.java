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
    name = "course",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = "reference"),
      @UniqueConstraint(columnNames = "title")
    })
@Getter
@Setter
public class Course {

  @Id
  @Column(length = 36)
  private UUID id;

  @Column(length = 20, nullable = false)
  private String reference;

  @Column(length = 50, nullable = false)
  private String title;

  @Column(nullable = false)
  private Short credits;
}
