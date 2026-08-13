package com.unigrade.api.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
  private String id;
  private OffsetDateTime examDate;
  private BigDecimal coefficient;
  private String courseId;
}
