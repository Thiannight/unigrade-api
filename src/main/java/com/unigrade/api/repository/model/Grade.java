package com.unigrade.api.repository.model;

import com.unigrade.api.PojaGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@PojaGenerated
@Entity
@Table(name = "grade")
@Getter
@Setter
public class Grade {

  @Id
  @Column(length = 50)
  private String id;

  @Column(nullable = false)
  private Float score;

  @Column(name = "grade_date", nullable = false)
  private OffsetDateTime gradeDate;

  @Column(length = 50)
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_id", nullable = false)
  private Exam exam;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;
}
