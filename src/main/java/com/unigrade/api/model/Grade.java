package com.unigrade.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Grade(
    UUID id,
    Float score,
    OffsetDateTime gradeDate,
    String reason,
    String studentId,
    UUID examId) {}
