package com.unigrade.api.model;

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
public class Grade {
  private String id;
  private Float score;
  private OffsetDateTime gradeDate;
  private String reason;
  private String examId;
  private String userId;
}
