package com.unigrade.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record GroupCourse(
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID id,
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) UUID groupId,
    @NotNull UUID courseId,
    @NotNull LocalDate startDate,
    LocalDate endDate) {}
