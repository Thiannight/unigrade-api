package com.unigrade.api.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record GradeRequest(
    @NotBlank(message = "studentId is required")
        @Pattern(regexp = "(STD)\\d{5}", message = "studentId must be STD followed by 5 digits")
        String studentId,
    @NotNull(message = "score is required")
        @Positive(message = "score must be positive")
        @DecimalMax(value = "20", message = "score must be at most 20")
        Float score,
    @NotNull(message = "gradeDate is required") Instant gradeDate,
    @NotBlank(message = "reason is required")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason) {}
