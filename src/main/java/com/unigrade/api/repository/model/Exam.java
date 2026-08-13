package com.unigrade.api.repository.model;

import com.unigrade.api.PojaGenerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@PojaGenerated
@Entity
@Table(name = "exam")
@Getter
@Setter
public class Exam {

  @Id
  @Column(length = 50)
  private String id;

  @Column(name = "exam_date", nullable = false)
  private OffsetDateTime examDate;

  @Column(precision = 5, scale = 4, nullable = false)
  private BigDecimal coefficient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;
}
