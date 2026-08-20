package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentGroup(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @NotBlank(message = "reference is required")
        @Pattern(regexp = "[A-Z][1-9]", message = "reference must be a letter followed by a digit")
        String reference,
    @NotNull(message = "promotionId is required") UUID promotionId) {}
