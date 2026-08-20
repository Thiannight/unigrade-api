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
    @NotBlank(message = "reference is required")
        @Size(max = 50, message = "reference must be at most 50 characters")
        String reference,
    @NotNull(message = "startYear is required") @Positive(message = "startYear must be positive")
        Short startYear,
    @NotNull(message = "endYear is required") @Positive(message = "endYear must be positive")
        Short endYear) {

  @JsonIgnore
  @AssertTrue(message = "startYear must be strictly before endYear")
  public boolean isYearRangeValid() {
    return startYear == null || endYear == null || startYear < endYear;
  }
}
