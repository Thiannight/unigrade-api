package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Membership(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID groupId,
    @NotBlank @Pattern(regexp = "(STD)\\d{5}") String studentId,
    @NotNull LocalDate startDate,
    LocalDate endDate) {}
