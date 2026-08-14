package com.unigrade.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record GroupAssignRequest(
    @NotBlank @Pattern(regexp = "(STD)\\d{5}") String studentId, @NotNull LocalDate startDate) {}
