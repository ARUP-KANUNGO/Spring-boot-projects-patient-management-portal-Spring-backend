package com.elrics.patientportal.repository;

import com.elrics.patientportal.entity.Patient;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);

    Optional<Patient> findByEmailIgnoreCase(String email);

    Optional<Patient> findByFullNameIgnoreCaseAndPhone(String fullName, String phone);

    long countByStatusIn(Collection<String> statuses);

    long countByAdmittedAtGreaterThanEqualAndAdmittedAtLessThan(LocalDateTime start, LocalDateTime end);

    long countByDischargedAtGreaterThanEqualAndDischargedAtLessThan(LocalDateTime start, LocalDateTime end);

    List<Patient> findTop5ByOrderByUpdatedAtDescIdDesc();

    @Query("SELECT p FROM Patient p WHERE "
            + "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(p.patientId) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "ORDER BY p.createdAt DESC, p.id DESC")
    List<Patient> search(@Param("q") String q);
}
