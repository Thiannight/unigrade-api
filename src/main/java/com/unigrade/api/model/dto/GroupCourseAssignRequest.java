package com.unigrade.api.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GroupCourseAssignRequest(@NotNull UUID courseId, @NotNull LocalDate startDate) {}
