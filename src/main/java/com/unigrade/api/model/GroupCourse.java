package com.unigrade.api.model;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record GroupCourse(UUID id, @NotNull UUID courseId, @NotNull UUID groupId) {}
