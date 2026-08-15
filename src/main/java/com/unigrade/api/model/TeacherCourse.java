package com.unigrade.api.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TeacherCourse(
    UUID id,
    @NotNull UUID courseId,
    @NotBlank @Pattern(regexp = "(TCR)\\d{5}") String teacherId,
    @NotNull @Positive @Min(1) @Max(5) Byte priority) {}
