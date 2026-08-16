package com.unigrade.api.model.dto;

import com.unigrade.api.model.Semester;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GroupCourseAssignRequest(
    @NotNull UUID courseId, @NotNull Semester semester, @NotNull LocalDate startDate) {}
