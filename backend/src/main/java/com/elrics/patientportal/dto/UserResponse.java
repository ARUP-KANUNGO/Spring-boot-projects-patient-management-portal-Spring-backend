package com.elrics.patientportal.dto;

import com.elrics.patientportal.entity.User;
import java.time.Instant;
import java.time.ZoneId;

public record UserResponse(Long id, String fullName, String username, String email, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        Instant created = user.getCreatedAt() == null ? null : user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
        return new UserResponse(user.getId(), user.getFullName(), user.getUsername(), user.getEmail(), user.getRole(), created);
    }
}
