package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID examId,
    @NotNull @Positive @DecimalMax("20") Float score,
    @NotNull Instant gradeDate,
    @NotBlank @Size(max = 255) String reason,
    @NotBlank @Pattern(regexp = "(STD)\\d{5}") String studentId) {}
