package com.unigrade.api.model;

import java.math.BigDecimal;
import java.util.List;

public record StudentReport(
    String studentId, List<LevelReport> levels, BigDecimal overallAverage) {}
