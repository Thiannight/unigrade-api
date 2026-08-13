package com.unigrade.api.model;

import java.time.LocalDate;

public record Membership(String id, LocalDate startDate, String groupId, String userId) {}
