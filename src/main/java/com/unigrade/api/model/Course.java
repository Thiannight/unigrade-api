package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Course(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @NotBlank(message = "reference is required")
        @Size(max = 20, message = "reference must be at most 20 characters")
        String reference,
    @NotBlank(message = "title is required")
        @Size(max = 50, message = "title must be at most 50 characters")
        String title,
    @NotNull(message = "credits is required") @Positive(message = "credits must be positive")
        Short credits) {}
