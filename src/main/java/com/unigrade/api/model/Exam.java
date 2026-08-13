package com.unigrade.api.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Exam(String id, OffsetDateTime examDate, BigDecimal coefficient, String courseId) {}
