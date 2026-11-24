package com.elrics.patientportal.service;

import com.elrics.patientportal.dto.DashboardStatsResponse;
import com.elrics.patientportal.dto.DashboardStatsResponse.RecentActivity;
import com.elrics.patientportal.dto.PatientResponse;
import com.elrics.patientportal.entity.Patient;
import com.elrics.patientportal.repository.PatientRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final PatientRepository patientRepository;

    public DashboardService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        long total = patientRepository.count();
        long admissionsToday = patientRepository
                .countByAdmittedAtGreaterThanEqualAndAdmittedAtLessThan(startOfToday, startOfTomorrow);
        long dischargedToday = patientRepository
                .countByDischargedAtGreaterThanEqualAndDischargedAtLessThan(startOfToday, startOfTomorrow);
        long pendingReviews = patientRepository
                .countByStatusIn(List.of(Patient.STATUS_PENDING, Patient.STATUS_UNDER_REVIEW));
        long currentlyAdmitted = patientRepository.countByStatusIn(List.of(Patient.STATUS_ADMITTED));

        List<RecentActivity> recent = patientRepository.findTop5ByOrderByUpdatedAtDescIdDesc().stream()
                .map(p -> new RecentActivity(p.getPatientId(), p.getFullName(), p.getStatus(), eventOf(p),
                        PatientResponse.toInstant(p.getUpdatedAt())))
                .toList();

        return new DashboardStatsResponse(total, admissionsToday, pendingReviews, dischargedToday,
                currentlyAdmitted, recent);
    }

    private static String eventOf(Patient p) {
        if (p.getCreatedAt() != null && p.getUpdatedAt() != null
                && Duration.between(p.getCreatedAt(), p.getUpdatedAt()).abs().getSeconds() < 2) {
            return "registered";
        }
        return "updated";
    }
}
