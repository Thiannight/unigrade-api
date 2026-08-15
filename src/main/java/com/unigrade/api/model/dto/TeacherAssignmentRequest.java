package com.unigrade.api.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TeacherAssignmentRequest(
    @NotBlank @Pattern(regexp = "(TCR)\\d{5}") String teacherId,
    @NotNull @Positive @Min(1) @Max(5) Byte priority) {}
