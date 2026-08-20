package com.unigrade.api.model.dto;

import com.unigrade.api.model.Semester;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GroupCourseAssignRequest(
    @NotNull(message = "courseId is required") UUID courseId,
    @NotNull(message = "semester is required") Semester semester,
    @NotNull(message = "startDate is required") LocalDate startDate) {}
