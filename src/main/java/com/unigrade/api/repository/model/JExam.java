package com.unigrade.api.repository.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JExam {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "exam_date", nullable = false)
  private Instant examDate;

  @Column(precision = 5, scale = 4, nullable = false)
  private BigDecimal coefficient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private JCourse course;

  @OneToMany(mappedBy = "exam")
  private List<JGrade> grades;
}
