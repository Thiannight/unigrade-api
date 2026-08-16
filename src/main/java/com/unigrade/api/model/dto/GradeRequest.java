package com.unigrade.api.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record GradeRequest(
    @NotBlank @Pattern(regexp = "(STD)\\d{5}") String studentId,
    @NotNull @Positive @DecimalMax("20") Float score,
    @NotNull Instant gradeDate,
    @NotBlank @Size(max = 255) String reason) {}
