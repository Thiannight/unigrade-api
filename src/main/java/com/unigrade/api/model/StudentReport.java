package com.unigrade.api.model;

import java.math.BigDecimal;
import java.util.List;

public record StudentReport(
    String studentId,
    String firstName,
    String lastName,
    List<LevelReport> levels,
    BigDecimal overallAverage) {}
