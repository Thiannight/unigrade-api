package com.unigrade.api.model;

import java.time.Instant;
import java.util.UUID;

public record Grade(
    UUID id, Float score, Instant gradeDate, String reason, String studentId, UUID examId) {}
