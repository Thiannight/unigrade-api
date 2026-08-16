package com.unigrade.api.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CourseReportEntry(
    UUID courseId,
    String promotionReference,
    String reference,
    String title,
    Short credits,
    boolean completed,
    BigDecimal average,
    List<ExamScore> exams) {}
