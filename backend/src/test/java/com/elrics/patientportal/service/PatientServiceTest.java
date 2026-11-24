package com.elrics.patientportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elrics.patientportal.dto.PatientRequest;
import com.elrics.patientportal.dto.PatientResponse;
import com.elrics.patientportal.entity.Patient;
import com.elrics.patientportal.entity.User;
import com.elrics.patientportal.exception.DuplicateResourceException;
import com.elrics.patientportal.exception.ResourceNotFoundException;
import com.elrics.patientportal.repository.PatientRepository;
import com.elrics.patientportal.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private PatientService patientService;

    private static PatientRequest request(String status) {
        return new PatientRequest("  Jane Smith ", 34, "Female", "+1 555-0101", "Jane@Example.com",
                "12 Main St", "O+", status, "John - 555-0102");
    }

    private static Patient existing(String patientId, String status) {
        Patient p = new Patient();
        p.setId(3L);
        p.setPatientId(patientId);
        p.setFullName("Jane Smith");
        p.setStatus(status);
        return p;
    }

    @Test
    void createGeneratesSequentialPatientIdAndNormalisesInput() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(patientRepository.saveAndFlush(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = patientService.create(request(null), 1L);

        assertEquals("PT-000007", response.id());
        assertEquals("Jane Smith", response.fullName());
        assertEquals("jane@example.com", response.email());
        assertEquals("Pending", response.status());
    }

    @Test
    void createAsAdmittedRecordsAdmissionTime() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(patientRepository.saveAndFlush(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        patientService.create(request("Admitted"), 1L);

        org.mockito.ArgumentCaptor<Patient> captor = org.mockito.ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).saveAndFlush(captor.capture());
        assertNotNull(captor.getValue().getAdmittedAt());
        assertNull(captor.getValue().getDischargedAt());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(patientRepository.findByEmailIgnoreCase("jane@example.com"))
                .thenReturn(Optional.of(existing("PT-000001", "Pending")));

        assertThrows(DuplicateResourceException.class, () -> patientService.create(request("Pending"), 1L));
        verify(patientRepository, never()).saveAndFlush(any(Patient.class));
    }

    @Test
    void createRejectsDuplicateNameAndPhone() {
        when(patientRepository.findByFullNameIgnoreCaseAndPhone("Jane Smith", "+1 555-0101"))
                .thenReturn(Optional.of(existing("PT-000001", "Pending")));

        assertThrows(DuplicateResourceException.class, () -> patientService.create(request("Pending"), 1L));
    }

    @Test
    void updateToDischargedRecordsDischargeTimeAndAllowsOwnEmail() {
        Patient patient = existing("PT-000003", "Admitted");
        when(patientRepository.findByPatientId("PT-000003")).thenReturn(Optional.of(patient));
        // the only match for this email is the patient being edited -> not a duplicate
        when(patientRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientResponse response = patientService.update("PT-000003", request("Discharged"));

        assertEquals("Discharged", response.status());
        assertNotNull(patient.getDischargedAt());
    }

    @Test
    void updateRejectsEmailOwnedByAnotherPatient() {
        Patient patient = existing("PT-000003", "Pending");
        when(patientRepository.findByPatientId("PT-000003")).thenReturn(Optional.of(patient));
        when(patientRepository.findByEmailIgnoreCase("jane@example.com"))
                .thenReturn(Optional.of(existing("PT-000009", "Pending")));

        assertThrows(DuplicateResourceException.class, () -> patientService.update("PT-000003", request("Pending")));
    }

    @Test
    void getMissingPatientThrowsNotFound() {
        when(patientRepository.findByPatientId("PT-999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.getByPatientId("PT-999999"));
    }

    @Test
    void deleteMissingPatientThrowsNotFound() {
        when(patientRepository.findByPatientId("PT-999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> patientService.delete("PT-999999"));
        verify(patientRepository, never()).delete(any(Patient.class));
    }

    @Test
    void deleteRemovesExistingPatient() {
        Patient patient = existing("PT-000003", "Pending");
        when(patientRepository.findByPatientId("PT-000003")).thenReturn(Optional.of(patient));

        patientService.delete("PT-000003");

        verify(patientRepository).delete(patient);
    }

    @Test
    void searchWithBlankQueryFallsBackToFullList() {
        when(patientRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());

        assertEquals(0, patientService.search("   ").size());
        verify(patientRepository, never()).search(any(String.class));
    }

    @Test
    void searchDelegatesToRepository() {
        when(patientRepository.search("jane")).thenReturn(List.of(existing("PT-000003", "Pending")));

        List<PatientResponse> results = patientService.search(" jane ");

        assertEquals(1, results.size());
        assertEquals("PT-000003", results.get(0).id());
    }
}
