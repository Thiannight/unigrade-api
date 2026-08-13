package com.unigrade.api.model;

import java.time.LocalDate;

public record User(
    String id,
    String firstName,
    String lastName,
    LocalDate birthDate,
    String email,
    String password,
    Boolean isActive,
    Role role) {}
