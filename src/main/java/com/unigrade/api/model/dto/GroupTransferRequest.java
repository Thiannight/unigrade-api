package com.unigrade.api.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record GroupTransferRequest(@NotNull UUID newGroupId, @NotNull LocalDate transferDate) {}
