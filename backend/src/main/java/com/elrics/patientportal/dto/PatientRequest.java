package com.elrics.patientportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Field names mirror the "name" attributes of the form in patients.html. */
public record PatientRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotNull(message = "Age is required")
        @Min(value = 0, message = "Age must be between 0 and 150")
        @Max(value = 150, message = "Age must be between 0 and 150")
        Integer age,

        @NotBlank(message = "Gender is required")
        @Pattern(regexp = "Male|Female|Other", message = "Gender must be Male, Female or Other")
        String gender,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9()\\-\\s.]{7,20}$", message = "Phone number is invalid (7-20 digits, may start with +)")
        String phone,

        @Email(message = "Please enter a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @NotBlank(message = "Blood group is required")
        @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood group must be one of A+, A-, B+, B-, AB+, AB-, O+, O-")
        String bloodGroup,

        @Pattern(regexp = "Pending|Admitted|Under Review|Discharged",
                message = "Status must be Pending, Admitted, Under Review or Discharged")
        String status,

        @Size(max = 150, message = "Emergency contact must not exceed 150 characters")
        String emergencyContact) {
}
