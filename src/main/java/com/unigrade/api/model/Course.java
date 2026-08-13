package com.unigrade.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Course(
    UUID id,
    @NotBlank @Size(max = 20) String reference,
    @NotBlank @Size(max = 50) String title,
    @NotNull @Positive Short credits) {}
