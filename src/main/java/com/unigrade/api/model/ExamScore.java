package com.unigrade.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExamScore(UUID examId, Instant examDate, BigDecimal coefficient, BigDecimal score) {}
