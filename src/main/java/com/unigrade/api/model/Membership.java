package com.unigrade.api.model;

import java.time.LocalDate;
import java.util.UUID;

public record Membership(
    UUID id, UUID groupId, String studentId, LocalDate startDate, LocalDate endDate) {}
