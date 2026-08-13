package com.unigrade.api.model;

import java.util.UUID;

public record Promotion(UUID id, String reference, Short startYear, Short endYear) {}
