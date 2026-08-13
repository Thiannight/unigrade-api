package com.unigrade.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Exam(UUID id, Instant examDate, BigDecimal coefficient, UUID courseId) {}
