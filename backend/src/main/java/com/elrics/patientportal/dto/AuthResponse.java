package com.elrics.patientportal.dto;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
