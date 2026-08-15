package com.unigrade.api.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeacherPriorityRequest(@NotNull @Positive @Min(1) @Max(5) Byte priority) {}
