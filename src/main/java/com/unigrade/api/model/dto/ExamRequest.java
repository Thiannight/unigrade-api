package com.unigrade.api.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record ExamRequest(
    @NotNull(message = "examDate is required") Instant examDate,
    @NotNull(message = "coefficient is required")
        @DecimalMin(value = "0", inclusive = false, message = "coefficient must be greater than 0")
        @DecimalMax(value = "1", message = "coefficient must be at most 1")
        BigDecimal coefficient) {}
