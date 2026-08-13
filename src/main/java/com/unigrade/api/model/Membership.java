package com.unigrade.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Membership(
    UUID id,
    @NotNull UUID groupId,
    @NotBlank @Pattern(regexp = "(STD|MGR|TCR)\\d{5}") String studentId,
    @NotNull LocalDate startDate,
    LocalDate endDate) {}
