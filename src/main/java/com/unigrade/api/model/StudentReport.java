package com.unigrade.api.model;

import java.math.BigDecimal;
import java.util.List;

public record StudentReport(
    String studentId,
    String firstName,
    String lastName,
    ReportStatus status,
    long earnedCredits,
    long requiredCredits,
    List<LevelReport> levels,
    BigDecimal overallAverage) {}
