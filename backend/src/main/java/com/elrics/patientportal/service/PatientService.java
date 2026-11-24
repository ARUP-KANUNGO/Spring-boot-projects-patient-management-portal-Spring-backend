package com.elrics.patientportal.service;

import com.elrics.patientportal.dto.PatientRequest;
import com.elrics.patientportal.dto.PatientResponse;
import com.elrics.patientportal.entity.Patient;
import com.elrics.patientportal.exception.DuplicateResourceException;
import com.elrics.patientportal.exception.ResourceNotFoundException;
import com.elrics.patientportal.repository.PatientRepository;
import com.elrics.patientportal.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public PatientService(PatientRepository patientRepository, UserRepository userRepository) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAll() {
        return patientRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream().map(PatientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse getByPatientId(String patientId) {
        return PatientResponse.from(find(patientId));
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return getAll();
        }
        return patientRepository.search(q).stream().map(PatientResponse::from).toList();
    }

    @Transactional
    public PatientResponse create(PatientRequest request, Long adminId) {
        String email = normalizeEmail(request.email());
        String fullName = request.fullName().trim();
        String phone = request.phone().trim();
        assertNoDuplicates(null, email, fullName, phone);

        Patient patient = new Patient();
        apply(patient, request, email, fullName, phone);
        // Temporary unique value; replaced by the real identifier once MySQL has assigned the numeric id.
        patient.setPatientId("TMP-" + UUID.randomUUID().toString().substring(0, 13));
        patient.setCreatedBy(userRepository.getReferenceById(adminId));

        Patient saved = patientRepository.saveAndFlush(patient);
        saved.setPatientId(String.format("PT-%06d", saved.getId()));
        return PatientResponse.from(patientRepository.save(saved));
    }

    @Transactional
    public PatientResponse update(String patientId, PatientRequest request) {
        Patient patient = find(patientId);
        String email = normalizeEmail(request.email());
        String fullName = request.fullName().trim();
        String phone = request.phone().trim();
        assertNoDuplicates(patient.getPatientId(), email, fullName, phone);

        apply(patient, request, email, fullName, phone);
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public void delete(String patientId) {
        patientRepository.delete(find(patientId));
    }

    private Patient find(String patientId) {
        return patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient " + patientId + " was not found"));
    }

    private void assertNoDuplicates(String currentPatientId, String email, String fullName, String phone) {
        if (email != null) {
            patientRepository.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getPatientId().equals(currentPatientId))
                    .ifPresent(other -> {
                        throw new DuplicateResourceException("A patient with this email address already exists");
                    });
        }
        patientRepository.findByFullNameIgnoreCaseAndPhone(fullName, phone)
                .filter(other -> !other.getPatientId().equals(currentPatientId))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(
                            "A patient with the same name and phone number already exists (" + other.getPatientId() + ")");
                });
    }

    private void apply(Patient patient, PatientRequest request, String email, String fullName, String phone) {
        patient.setFullName(fullName);
        patient.setAge(request.age());
        patient.setGender(request.gender());
        patient.setPhone(phone);
        patient.setEmail(email);
        patient.setAddress(blankToNull(request.address()));
        patient.setBloodGroup(request.bloodGroup());
        patient.setEmergencyContact(blankToNull(request.emergencyContact()));
        applyStatus(patient, request.status() == null || request.status().isBlank()
                ? Patient.STATUS_PENDING : request.status());
    }

    /** Records admission / discharge moments so the dashboard can report "today's" figures from the database. */
    private void applyStatus(Patient patient, String newStatus) {
        if (!newStatus.equals(patient.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            if (Patient.STATUS_ADMITTED.equals(newStatus)) {
                patient.setAdmittedAt(now);
            } else if (Patient.STATUS_DISCHARGED.equals(newStatus)) {
                patient.setDischargedAt(now);
            }
        }
        patient.setStatus(newStatus);
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
