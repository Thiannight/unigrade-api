package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Exam(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID groupCourseId,
    @NotNull Instant examDate,
    @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal coefficient) {}
