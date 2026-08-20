package com.unigrade.api.model;

import java.math.BigDecimal;
import java.util.List;

public record LevelReport(
    Level level,
    ReportStatus status,
    long earnedCredits,
    long requiredCredits,
    BigDecimal overallAverage,
    List<CourseReportEntry> courses) {}
