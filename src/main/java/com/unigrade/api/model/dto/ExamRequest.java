package com.unigrade.api.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record ExamRequest(
    @NotNull Instant examDate, @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal coefficient) {}
