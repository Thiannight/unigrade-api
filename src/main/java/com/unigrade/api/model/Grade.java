package com.unigrade.api.model;

import java.time.OffsetDateTime;

public record Grade(
    String id,
    Float score,
    OffsetDateTime gradeDate,
    String reason,
    String studentId,
    String examId) {}
