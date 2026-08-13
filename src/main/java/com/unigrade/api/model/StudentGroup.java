package com.unigrade.api.model;

import java.util.UUID;

public record StudentGroup(UUID id, String reference, UUID promotionId) {}
