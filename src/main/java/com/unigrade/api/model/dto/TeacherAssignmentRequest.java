package com.unigrade.api.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TeacherAssignmentRequest(
    @NotBlank(message = "teacherId is required")
        @Pattern(regexp = "(TCR)\\d{5}", message = "teacherId must be TCR followed by 5 digits")
        String teacherId,
    @NotNull(message = "priority is required")
        @Positive(message = "priority must be positive")
        @Min(value = 1, message = "priority must be between 1 and 5")
        @Max(value = 5, message = "priority must be between 1 and 5")
        Byte priority) {}
