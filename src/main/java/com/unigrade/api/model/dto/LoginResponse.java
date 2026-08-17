package com.unigrade.api.model.dto;

public record LoginResponse(String accessToken, String userId, String role) {}
