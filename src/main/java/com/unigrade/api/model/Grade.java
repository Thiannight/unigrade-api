package com.unigrade.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Grade(
    UUID id,
    @NotNull @Positive Float score,
    @NotNull Instant gradeDate,
    @NotBlank @Size(max = 255) String reason,
    @NotBlank @Pattern(regexp = "(STD|MGR|TCR)\\d{5}") String studentId,
    @NotNull UUID examId) {}
