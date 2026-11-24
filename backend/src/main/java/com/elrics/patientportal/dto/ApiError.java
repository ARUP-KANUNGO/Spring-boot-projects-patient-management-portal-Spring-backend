package com.elrics.patientportal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** Uniform error body returned for every failed request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String timestamp, int status, String error, String message,
                       Map<String, String> errors, String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, null, path);
    }

    public static ApiError of(HttpStatus status, String message, Map<String, String> errors, String path) {
        return new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, errors, path);
    }
}
