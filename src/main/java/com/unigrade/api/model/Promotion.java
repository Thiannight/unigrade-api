package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Promotion(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @NotBlank @Size(max = 50) String reference,
    @NotNull @Positive Short startYear,
    @NotNull @Positive Short endYear) {

  @JsonIgnore
  @AssertTrue(message = "startYear must be strictly before endYear")
  public boolean isYearRangeValid() {
    return startYear == null || endYear == null || startYear < endYear;
  }
}
