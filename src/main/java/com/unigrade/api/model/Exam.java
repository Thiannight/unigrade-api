package com.unigrade.api.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Exam(UUID id, OffsetDateTime examDate, BigDecimal coefficient, UUID courseId) {}
