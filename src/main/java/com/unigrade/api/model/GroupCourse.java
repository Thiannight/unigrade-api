package com.unigrade.api.model;

import java.util.UUID;

public record GroupCourse(UUID id, UUID courseId, UUID groupId) {}
