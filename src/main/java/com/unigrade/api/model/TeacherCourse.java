package com.unigrade.api.model;

import java.util.UUID;

public record TeacherCourse(UUID id, UUID courseId, String teacherId, Short schoolYear) {}
