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
    @NotBlank @Pattern(regexp = "[A-Z][1-9]") String reference,
    @NotNull UUID promotionId) {}
