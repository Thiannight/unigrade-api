package com.unigrade.api.model;

import java.time.LocalDate;

public record Membership(
    String id, String groupId, String studentId, LocalDate startDate, LocalDate endDate) {}
