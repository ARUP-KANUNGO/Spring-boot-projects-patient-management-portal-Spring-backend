package com.elrics.patientportal.dto;

import com.elrics.patientportal.entity.Patient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** "id" is the public patient identifier (e.g. PT-000042), which the frontend uses as the row key. */
public record PatientResponse(String id, String fullName, Integer age, String gender, String phone, String email,
                              String address, String bloodGroup, String status, String emergencyContact,
                              Instant createdAt, Instant updatedAt) {

    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getPatientId(), p.getFullName(), p.getAge(), p.getGender(), p.getPhone(),
                p.getEmail(), p.getAddress(), p.getBloodGroup(), p.getStatus(), p.getEmergencyContact(),
                toInstant(p.getCreatedAt()), toInstant(p.getUpdatedAt()));
    }

    public static Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
