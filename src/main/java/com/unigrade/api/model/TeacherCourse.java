package com.unigrade.api.model;

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
    @NotBlank @Pattern(regexp = "(STD|MGR|TCR)\\d{5}") String teacherId,
    @NotNull @Positive Short schoolYear) {}
