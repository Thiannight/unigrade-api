package com.unigrade.api.model;

import java.math.BigDecimal;

public record GraduationListEntry(
    int rank, String studentId, String firstName, String lastName, BigDecimal allTimeAverage) {}
