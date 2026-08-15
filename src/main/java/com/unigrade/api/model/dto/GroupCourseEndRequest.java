package com.unigrade.api.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GroupCourseEndRequest(@NotNull LocalDate endDate) {}
