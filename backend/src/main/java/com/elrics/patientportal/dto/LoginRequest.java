package com.elrics.patientportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "username" accepts either the username or the e-mail address (the login form has a single field). */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 150, message = "Username must not exceed 150 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password must not exceed 72 characters")
        String password) {
}
