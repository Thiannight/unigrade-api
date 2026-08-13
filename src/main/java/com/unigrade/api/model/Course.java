package com.unigrade.api.model;

import java.util.UUID;

public record Course(UUID id, String reference, String title, Short credits) {}
