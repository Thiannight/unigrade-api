package com.unigrade.api.repository.model;

import com.unigrade.api.PojaGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@PojaGenerated
@Entity
@Table(name = "teach")
@Getter
@Setter
public class Teach {

  @Id
  @Column(length = 50)
  private String id;

  @Column(name = "school_year", nullable = false)
  private Short schoolYear;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;
}
