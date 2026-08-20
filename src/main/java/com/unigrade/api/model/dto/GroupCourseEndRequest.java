package com.unigrade.api.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GroupCourseEndRequest(@NotNull(message = "endDate is required") LocalDate endDate) {}
