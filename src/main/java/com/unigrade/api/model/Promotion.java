package com.unigrade.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Promotion(
    UUID id,
    @NotBlank @Size(max = 50) String reference,
    @NotNull @Positive Short startYear,
    @NotNull @Positive Short endYear) {}
