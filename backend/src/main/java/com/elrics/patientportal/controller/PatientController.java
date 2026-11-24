package com.elrics.patientportal.controller;

import com.elrics.patientportal.dto.PatientRequest;
import com.elrics.patientportal.dto.PatientResponse;
import com.elrics.patientportal.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {id} in every path is the public patient identifier returned as "id" (e.g. PT-000042). */
@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Create a patient")
    @PostMapping
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request,
                                                  Authentication authentication) {
        PatientResponse created = patientService.create(request, Long.valueOf(authentication.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List all patients (newest first)")
    @GetMapping
    public List<PatientResponse> getAll() {
        return patientService.getAll();
    }

    @Operation(summary = "Search patients by name, patient ID, phone or email")
    @GetMapping("/search")
    public List<PatientResponse> search(@RequestParam("query") String query) {
        return patientService.search(query);
    }

    @Operation(summary = "Get one patient")
    @GetMapping("/{id}")
    public PatientResponse getOne(@PathVariable("id") String id) {
        return patientService.getByPatientId(id);
    }

    @Operation(summary = "Update a patient")
    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable("id") String id, @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request);
    }

    @Operation(summary = "Delete a patient")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
